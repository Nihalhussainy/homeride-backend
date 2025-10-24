package com.homeride.backend.service;

import com.google.maps.model.LatLng;
import com.homeride.backend.dto.RideRequestDTO;
import com.homeride.backend.dto.StopoverDto;
import com.homeride.backend.dto.TravelInfo;
import com.homeride.backend.model.Employee;
import com.homeride.backend.model.RideParticipant;
import com.homeride.backend.model.RideRequest;
import com.homeride.backend.model.Stopover;
import com.homeride.backend.repository.EmployeeRepository;
import com.homeride.backend.repository.RideParticipantRepository;
import com.homeride.backend.repository.RideRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class RideRequestService {

    private static final Logger logger = LoggerFactory.getLogger(RideRequestService.class);

    private final RideRequestRepository rideRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final RideParticipantRepository rideParticipantRepository;
    private final GoogleMapsService googleMapsService;
    private final RatingService ratingService;
    private final NotificationService notificationService;
    private final PricingService pricingService;

    @Autowired
    public RideRequestService(RideRequestRepository rideRequestRepository,
                              EmployeeRepository employeeRepository,
                              RideParticipantRepository rideParticipantRepository,
                              GoogleMapsService googleMapsService,
                              RatingService ratingService,
                              NotificationService notificationService,
                              PricingService pricingService) {
        this.rideRequestRepository = rideRequestRepository;
        this.employeeRepository = employeeRepository;
        this.rideParticipantRepository = rideParticipantRepository;
        this.googleMapsService = googleMapsService;
        this.ratingService = ratingService;
        this.notificationService = notificationService;
        this.pricingService = pricingService;
    }

    @Transactional
    public RideRequest createRideOffer(RideRequestDTO rideRequestDTO, String requesterEmail) {
        Employee requester = employeeRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new RuntimeException("Employee not found with email: " + requesterEmail));

        RideRequest newRideOffer = new RideRequest();
        newRideOffer.setOriginCity(rideRequestDTO.getOriginCity());
        newRideOffer.setOrigin(rideRequestDTO.getOrigin());
        newRideOffer.setDestinationCity(rideRequestDTO.getDestinationCity());
        newRideOffer.setDestination(rideRequestDTO.getDestination());
        newRideOffer.setTravelDateTime(rideRequestDTO.getTravelDateTime());
        newRideOffer.setStatus("PENDING");
        newRideOffer.setRequester(requester);
        newRideOffer.setRideType("OFFERED");
        newRideOffer.setVehicleModel(rideRequestDTO.getVehicleModel());
        newRideOffer.setVehicleCapacity(rideRequestDTO.getVehicleCapacity());
        newRideOffer.setGenderPreference(rideRequestDTO.getGenderPreference());
        newRideOffer.setDriverNote(rideRequestDTO.getDriverNote());

        // Process Stops
        List<Stopover> stopoverEntities = new ArrayList<>();
        if (rideRequestDTO.getStops() != null) {
            stopoverEntities = rideRequestDTO.getStops().stream()
                    .filter(dto -> dto.getPoint() != null && !dto.getPoint().trim().isEmpty())
                    .map(dto -> {
                        Stopover stopover = new Stopover();
                        stopover.setCity(dto.getCity());
                        stopover.setPoint(dto.getPoint());
                        stopover.setRideRequest(newRideOffer);
                        try {
                            LatLng location = googleMapsService.geocodeAddress(dto.getPoint());
                            if (location != null) {
                                stopover.setLat(location.lat);
                                stopover.setLng(location.lng);
                            }
                        } catch (Exception e) {
                            logger.error("Could not geocode stopover point: {}", dto.getPoint(), e);
                        }
                        return stopover;
                    })
                    .collect(Collectors.toList());
            newRideOffer.setStopovers(stopoverEntities);
        }

        // ===== KEY CHANGE: GET DIRECT DISTANCE FOR PRICING =====
        // This is independent of stopovers
        double directDistance = googleMapsService.getDirectDistance(
                rideRequestDTO.getOrigin(),
                rideRequestDTO.getDestination()
        );
        logger.info("Direct Distance (for pricing): {:.2f}km", directDistance);

        // ===== GET FULL ROUTE INFO WITH STOPOVERS (for display & segment pricing) =====
        String[] stopsArray = stopoverEntities.stream()
                .map(Stopover::getPoint)
                .toArray(String[]::new);
        TravelInfo travelInfo = googleMapsService.getTravelInfoWithStopovers(
                rideRequestDTO.getOrigin(),
                rideRequestDTO.getDestination(),
                stopsArray
        );

        double actualRouteDistance = travelInfo.getDistanceInKm();
        newRideOffer.setDuration(travelInfo.getDurationInMinutes());
        newRideOffer.setDistance(actualRouteDistance); // Store actual route distance
        newRideOffer.setRoutePolyline(travelInfo.getPolyline());

        // ===== PRICING: USE DIRECT DISTANCE ONLY =====
        int numberOfSegments = stopoverEntities.size() + 1;

        // Calculate TOTAL RIDE price range based on DIRECT DISTANCE ONLY (stopovers don't affect pricing)
        PricingService.PriceRange totalRange = pricingService.getTotalPriceRange(directDistance);

        double finalTotalPrice = (rideRequestDTO.getPrice() != null &&
                rideRequestDTO.getPrice() >= totalRange.minPrice &&
                rideRequestDTO.getPrice() <= totalRange.maxPrice)
                ? rideRequestDTO.getPrice()
                : totalRange.recommendedPrice;

        newRideOffer.setPrice(finalTotalPrice);

        if (directDistance > 0) {
            newRideOffer.setPricePerKm(finalTotalPrice / directDistance);
        } else {
            newRideOffer.setPricePerKm(0.0);
        }

        logger.info("Total Ride Pricing (Direct Distance ONLY): DirectDistance={}km, ActualRoute={}km, Min={}, Recommended={}, Max={}, Set={}",
                directDistance, actualRouteDistance, totalRange.minPrice, totalRange.recommendedPrice, totalRange.maxPrice, finalTotalPrice);

        // ===== SEGMENT PRICING: INDEPENDENT CALCULATION =====
        List<Double> segmentPrices = new ArrayList<>();
        List<String> segmentInfo = new ArrayList<>();

        // Get segment distances from the actual route (with stopovers)
        List<Double> segmentDistances = new ArrayList<>();
        if (travelInfo.getSegmentDistances() != null && !travelInfo.getSegmentDistances().isEmpty()) {
            segmentDistances.addAll(travelInfo.getSegmentDistances());
        } else {
            // Fallback: estimate proportionally based on direct distance
            double perSegment = directDistance / numberOfSegments;
            for (int i = 0; i < numberOfSegments; i++) {
                segmentDistances.add(perSegment);
            }
        }

        // Calculate price for each segment independently based on its OWN distance
        for (int i = 0; i < numberOfSegments; i++) {
            double segmentDist = segmentDistances.get(i);

            // Get segment-specific pricing range based on SEGMENT DISTANCE ONLY
            PricingService.PriceRange segmentRange = pricingService.getSegmentPriceRange(segmentDist);

            double segmentPrice;
            if (rideRequestDTO.getStopoverPrices() != null &&
                    i < rideRequestDTO.getStopoverPrices().size()) {
                // Use provided price, but clamp to segment range
                double providedPrice = rideRequestDTO.getStopoverPrices().get(i);
                segmentPrice = Math.max(segmentRange.minPrice,
                        Math.min(segmentRange.maxPrice, providedPrice));
                segmentPrice = Math.round(segmentPrice / 10.0) * 10.0;
            } else {
                // Use recommended price for this segment
                segmentPrice = segmentRange.recommendedPrice;
            }

            segmentPrices.add(segmentPrice);
            segmentInfo.add(String.format("Segment %d: %.0fkm, Range(%.0f-%.0f), Set=%.0f",
                    i + 1, segmentDist, segmentRange.minPrice, segmentRange.maxPrice, segmentPrice));
        }

        newRideOffer.setStopoverPrices(segmentPrices);
        segmentInfo.forEach(logger::info);

        // Save ride
        RideRequest savedRide = rideRequestRepository.save(newRideOffer);

        // Notification
        String message = "You offered a ride from " + savedRide.getOriginCity() +
                " to " + savedRide.getDestinationCity();
        notificationService.createNotification(requester, message, "/ride/" + savedRide.getId(),
                "RIDE_OFFERED", savedRide.getId());

        logger.info("Created Ride Offer ID: {}, Direct Distance: {}km, Actual Route: {}km, Total Price: {}, Segment Prices: {}",
                savedRide.getId(), directDistance, actualRouteDistance, savedRide.getPrice(), savedRide.getStopoverPrices());

        return savedRide;
    }
    public RideRequest getRideById(Long rideId) {
        RideRequest ride = rideRequestRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found with id: " + rideId));

        if (ride.getRequester() != null) {
            Double avgRating = ratingService.calculateAverageRating(ride.getRequester().getId());
            ride.getRequester().setAverageRating(avgRating);
            ride.getParticipants().size();
        }
        ride.getStopovers().size();

        return ride;
    }

    public List<RideRequest> getAllRideRequests(String origin, String destination, String travelDateTime, Integer passengerCount) {
        List<RideRequest> rides = rideRequestRepository.findAll().stream()
                .filter(r -> "OFFERED".equals(r.getRideType()) && r.getTravelDateTime().isAfter(LocalDateTime.now()))
                .collect(Collectors.toList());

        List<RideRequest> filteredRides = rides.stream()
                .filter(ride -> {
                    if (travelDateTime != null && !travelDateTime.trim().isEmpty()) {
                        try {
                            LocalDate searchDate = LocalDate.parse(travelDateTime);
                            if (!ride.getTravelDateTime().toLocalDate().isEqual(searchDate)) {
                                return false;
                            }
                        } catch (Exception e) {
                            logger.warn("Invalid date format during search: {}", travelDateTime);
                        }
                    }

                    if (passengerCount != null && passengerCount > 0) {
                        int totalSeatsBooked = ride.getParticipants().stream()
                                .mapToInt(p -> p.getNumberOfSeats() != null ? p.getNumberOfSeats() : 1)
                                .sum();
                        int availableSeats = ride.getVehicleCapacity() - totalSeatsBooked;
                        if (availableSeats < passengerCount) {
                            return false;
                        }
                    }

                    if (origin != null && !origin.trim().isEmpty() &&
                            destination != null && !destination.trim().isEmpty()) {
                        if (!canAccommodateJourney(ride, origin, destination)) {
                            return false;
                        }
                    }

                    return true;
                })
                .collect(Collectors.toList());

        for (RideRequest ride : filteredRides) {
            if (ride.getRequester() != null) {
                Double avgRating = ratingService.calculateAverageRating(ride.getRequester().getId());
                ride.getRequester().setAverageRating(avgRating);
            }
        }

        return filteredRides;
    }

    private boolean canAccommodateJourney(RideRequest ride, String searchOrigin, String searchDestination) {
        List<RoutePoint> fullPath = buildFullPath(ride);

        logger.debug("=== Checking Ride ID: {} for Journey: '{}' -> '{}' ===", ride.getId(), searchOrigin, searchDestination);
        fullPath.forEach(p -> logger.debug("  Route Point: City='{}', Point='{}'", p.getCity(), p.getPoint()));

        int originIndex = -1;
        int destinationIndex = -1;

        for (int i = 0; i < fullPath.size(); i++) {
            if (matchesLocation(fullPath.get(i), searchOrigin)) {
                originIndex = i;
                logger.debug("  Origin found at index {}", i);
                break;
            }
        }

        if (originIndex == -1) {
            logger.debug("  Origin not found in route.");
            return false;
        }

        for (int i = originIndex + 1; i < fullPath.size(); i++) {
            if (matchesLocation(fullPath.get(i), searchDestination)) {
                destinationIndex = i;
                logger.debug("  Destination found at index {}", i);
                break;
            }
        }

        boolean possible = destinationIndex != -1;
        logger.debug("=== Journey Possible: {} ===\n", possible);
        return possible;
    }

    private List<RoutePoint> buildFullPath(RideRequest ride) {
        List<RoutePoint> fullPath = new ArrayList<>();
        fullPath.add(new RoutePoint(ride.getOriginCity(), ride.getOrigin()));
        if (ride.getStopovers() != null) {
            ride.getStopovers().forEach(stop -> fullPath.add(new RoutePoint(stop.getCity(), stop.getPoint())));
        }
        fullPath.add(new RoutePoint(ride.getDestinationCity(), ride.getDestination()));
        return fullPath;
    }

    private boolean matchesLocation(RoutePoint routePoint, String searchLocation) {
        if (searchLocation == null || searchLocation.trim().isEmpty() || routePoint == null) {
            return false;
        }

        String searchLower = normalizeLocation(searchLocation);
        String cityLower = normalizeLocation(routePoint.getCity());
        String pointLower = normalizeLocation(routePoint.getPoint());

        logger.trace("  Comparing search='{}' with city='{}', point='{}'", searchLower, cityLower, pointLower);

        if (!pointLower.isEmpty()) {
            if (pointLower.equals(searchLower)) return true;
            if (pointLower.contains(searchLower)) return true;
            if (searchLower.contains(pointLower)) return true;
        }

        if (!cityLower.isEmpty()) {
            if (cityLower.equals(searchLower)) return true;
            if (cityLower.contains(searchLower) && searchLower.length() > 2) return true;
            if (searchLower.contains(cityLower) && cityLower.length() > 2) return true;
        }

        String searchMainCity = extractMainCity(searchLocation);
        String cityMainCity = extractMainCity(routePoint.getCity());
        String pointMainCity = extractMainCity(routePoint.getPoint());

        if (!searchMainCity.isEmpty() && searchMainCity.length() >= 3) {
            if (!cityMainCity.isEmpty() && cityMainCity.contains(searchMainCity)) return true;
            if (!pointMainCity.isEmpty() && pointMainCity.contains(searchMainCity)) return true;
            if (!cityMainCity.isEmpty() && searchMainCity.contains(cityMainCity)) return true;
        }

        return false;
    }

    private String normalizeLocation(String location) {
        if (location == null) return "";
        return location.toLowerCase()
                .trim()
                .replaceAll("\\s+", " ")
                .replace(", india", "")
                .replace(", maharashtra", "")
                .replace(", tamil nadu", "")
                .replace(", andhra pradesh", "");
    }

    private String extractMainCity(String location) {
        if (location == null || location.isEmpty()) return "";
        String normalized = normalizeLocation(location);
        String[] parts = normalized.split(",");
        return (parts.length > 0 && parts[0].trim().length() >= 3) ? parts[0].trim() : normalized;
    }

    @Transactional
    public void deleteRide(Long rideId, String userEmail) {
        RideRequest ride = rideRequestRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found with id: " + rideId));
        Employee user = employeeRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + userEmail));

        if (!Objects.equals(ride.getRequester().getId(), user.getId())) {
            throw new IllegalStateException("You are not authorized to delete this ride.");
        }

        ride.getParticipants().forEach(participant -> {
            String message = "Your ride from " + ride.getOriginCity() + " to " + ride.getDestinationCity() + " has been canceled by the driver.";
            notificationService.createNotification(participant.getParticipant(), message, "/dashboard", "RIDE_CANCELED", ride.getId());
        });

        ratingService.deleteAllRatingsForRide(ride);
        logger.info("User {} authorized. Deleting ride ID: {}", userEmail, rideId);
        rideRequestRepository.delete(ride);
    }

    @Transactional
    public RideParticipant joinRideRequest(Long rideId, String participantEmail, Map<String, Object> segmentDetails) {
        RideRequest rideRequest = rideRequestRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found with id: " + rideId));
        Employee participant = employeeRepository.findByEmail(participantEmail)
                .orElseThrow(() -> new RuntimeException("Employee not found with email: " + participantEmail));

        String pickupPoint = (String) segmentDetails.get("pickupPoint");
        String dropoffPoint = (String) segmentDetails.get("dropoffPoint");
        Double price = ((Number) segmentDetails.get("price")).doubleValue();
        Integer numberOfSeats = 1;
        if (segmentDetails.containsKey("numberOfSeats")) {
            try {
                numberOfSeats = ((Number) segmentDetails.get("numberOfSeats")).intValue();
            } catch (Exception e) {
                logger.warn("Invalid numberOfSeats format, defaulting to 1.");
            }
        }

        if (pickupPoint == null || pickupPoint.trim().isEmpty() ||
                dropoffPoint == null || dropoffPoint.trim().isEmpty() ||
                price == null || numberOfSeats < 1) {
            throw new IllegalArgumentException("Pickup point, drop-off point, valid price, and number of seats must be provided.");
        }

        List<RoutePoint> fullPath = buildFullPath(rideRequest);
        int pickupIndex = -1;
        int dropoffIndex = -1;
        for(int i=0; i<fullPath.size(); i++) {
            if(pickupIndex == -1 && matchesLocation(fullPath.get(i), pickupPoint)) pickupIndex = i;
            if(pickupIndex != -1 && matchesLocation(fullPath.get(i), dropoffPoint)) {
                dropoffIndex = i;
                break;
            }
        }

        if (pickupIndex == -1 || dropoffIndex == -1) throw new IllegalStateException("Invalid pickup or drop-off point. Must match the route.");
        if (pickupIndex >= dropoffIndex) throw new IllegalStateException("Pickup point must be before drop-off point.");
        if (!"OFFERED".equalsIgnoreCase(rideRequest.getRideType())) throw new IllegalStateException("You can only join offered rides.");
        if ("FEMALE_ONLY".equalsIgnoreCase(rideRequest.getGenderPreference()) && !"FEMALE".equalsIgnoreCase(participant.getGender())) throw new IllegalStateException("This ride is for female participants only.");

        int totalSeatsBooked = rideRequest.getParticipants().stream()
                .mapToInt(p -> p.getNumberOfSeats() != null ? p.getNumberOfSeats() : 1)
                .sum();
        if (rideRequest.getVehicleCapacity() - totalSeatsBooked < numberOfSeats) {
            throw new IllegalStateException("Not enough seats available. Only " + (rideRequest.getVehicleCapacity() - totalSeatsBooked) + " seat(s) left.");
        }

        if (Objects.equals(rideRequest.getRequester().getId(), participant.getId())) throw new IllegalStateException("You cannot join your own ride.");
        if (rideParticipantRepository.existsByRideRequestAndParticipant(rideRequest, participant)) throw new IllegalStateException("You have already joined this ride.");

        RideParticipant rideParticipant = new RideParticipant();
        rideParticipant.setRideRequest(rideRequest);
        rideParticipant.setParticipant(participant);
        rideParticipant.setPickupPoint(fullPath.get(pickupIndex).getPoint());
        rideParticipant.setDropoffPoint(fullPath.get(dropoffIndex).getPoint());
        rideParticipant.setPrice(price);
        rideParticipant.setNumberOfSeats(numberOfSeats);

        RideParticipant savedParticipant = rideParticipantRepository.save(rideParticipant);

        String seatText = numberOfSeats > 1 ? numberOfSeats + " seats" : "1 seat";
        String driverMessage = participant.getName() + " booked " + seatText + " on your ride: " +
                rideRequest.getOriginCity() + " -> " + rideRequest.getDestinationCity() +
                " (Segment: " + extractMainCity(pickupPoint) + " -> " + extractMainCity(dropoffPoint) + ")";
        notificationService.createNotification(rideRequest.getRequester(), driverMessage, "/ride/" + rideId, "RIDE_JOINED", rideId);

        String participantMessage = "Booking confirmed for " + seatText + ": " +
                rideRequest.getOriginCity() + " -> " + rideRequest.getDestinationCity() +
                " (Your segment: " + extractMainCity(pickupPoint) + " -> " + extractMainCity(dropoffPoint) + ")";
        notificationService.createNotification(participant, participantMessage, "/ride/" + rideId, "RIDE_BOOKED", rideId);

        return savedParticipant;
    }

    public List<RideRequest> getRidesForUser(String userEmail) {
        Employee employee = employeeRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + userEmail));
        Long userId = employee.getId();

        logger.info("Fetching rides for user: {} (ID: {})", userEmail, userId);

        List<RideRequest> userRides = rideRequestRepository.findAll().stream()
                .filter(ride -> {
                    boolean isRequester = ride.getRequester() != null && ride.getRequester().getId().equals(userId);
                    boolean isParticipant = ride.getParticipants() != null && ride.getParticipants().stream()
                            .anyMatch(p -> p.getParticipant() != null && p.getParticipant().getId().equals(userId));

                    if(isRequester || isParticipant) {
                        logger.trace("  Ride ID {}: {} -> {} | {} | Role: {}",
                                ride.getId(), ride.getOriginCity(), ride.getDestinationCity(), ride.getTravelDateTime(), (isRequester ? "DRIVER" : "PASSENGER"));
                    }
                    return isRequester || isParticipant;
                })
                .collect(Collectors.toList());

        logger.info("Total rides found for user {}: {}", userEmail, userRides.size());

        userRides.forEach(ride -> {
            if (ride.getRequester() != null) {
                Double avgRating = ratingService.calculateAverageRating(ride.getRequester().getId());
                ride.getRequester().setAverageRating(avgRating);
            }
            ride.getParticipants().size();
            ride.getStopovers().size();
        });

        return userRides;
    }

    private static class RoutePoint {
        private final String city;
        private final String point;

        public RoutePoint(String city, String point) {
            this.city = city;
            this.point = point;
        }

        public String getCity() { return city; }
        public String getPoint() { return point; }

        @Override
        public String toString() { return "RoutePoint{city='" + city + "', point='" + point + "'}"; }
    }
}