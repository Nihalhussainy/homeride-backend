package com.homeride.backend.repository;

import com.homeride.backend.model.Employee;
import com.homeride.backend.model.RideRequest;
import com.homeride.backend.model.Stopover;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RideRequestRepository extends JpaRepository<RideRequest, Long>, JpaSpecificationExecutor<RideRequest> {

    // FIXED: Added "stopovers" to EntityGraph
    @Override
    @EntityGraph(attributePaths = {"requester", "driver", "participants.participant", "stopovers"})
    List<RideRequest> findAll();

    @EntityGraph(attributePaths = {"requester", "driver", "participants.participant", "stopovers"})
    List<RideRequest> findAll(Specification<RideRequest> spec);

    @EntityGraph(attributePaths = {"requester", "driver", "participants.participant", "stopovers"})
    Optional<RideRequest> findById(Long id);

    // FIXED: Updated to use stopovers instead of stops
    @Query("SELECT DISTINCT r FROM RideRequest r LEFT JOIN r.stopovers s WHERE " +
            "(LOWER(r.origin) LIKE LOWER(CONCAT('%', :location, '%')) OR " +
            "LOWER(r.destination) LIKE LOWER(CONCAT('%', :location, '%')) OR " +
            "LOWER(s.point) LIKE LOWER(CONCAT('%', :location, '%')))")
    List<RideRequest> findByLocationInPath(@Param("location") String location);

    // FIXED: Updated to use stopovers instead of stops
    @Query("SELECT DISTINCT r FROM RideRequest r LEFT JOIN r.stopovers s WHERE " +
            "((LOWER(r.origin) LIKE LOWER(CONCAT('%', :origin, '%')) OR " +
            "LOWER(s.point) LIKE LOWER(CONCAT('%', :origin, '%'))) AND " +
            "(LOWER(r.destination) LIKE LOWER(CONCAT('%', :destination, '%')) OR " +
            "LOWER(s.point) LIKE LOWER(CONCAT('%', :destination, '%'))))")
    List<RideRequest> findByOriginAndDestinationInPath(@Param("origin") String origin, @Param("destination") String destination);

    long countByRequester(Employee requester);
    long countByDriver(Employee driver);

    interface Ridespecs {

        static Specification<RideRequest> hasOrigin(String origin) {
            return (root, query, cb) -> cb.like(cb.lower(root.get("origin")), "%" + origin.toLowerCase() + "%");
        }

        static Specification<RideRequest> hasDestination(String destination) {
            return (root, query, cb) -> cb.like(cb.lower(root.get("destination")), "%" + destination.toLowerCase() + "%");
        }

        // FIXED: Updated to use stopovers entity instead of stops collection
        static Specification<RideRequest> hasStop(String stop) {
            return (root, query, cb) -> {
                var stopoverJoin = root.join("stopovers");
                return cb.like(cb.lower(stopoverJoin.get("point")), "%" + stop.toLowerCase() + "%");
            };
        }

        // FIXED: Updated to use stopovers instead of stops
        static Specification<RideRequest> passesThrough(String location) {
            return (root, query, cb) -> {
                var subquery = query.subquery(Long.class);
                var subRoot = subquery.from(Stopover.class);
                subquery.select(cb.literal(1L))
                        .where(
                                cb.equal(subRoot.get("rideRequest"), root),
                                cb.like(cb.lower(subRoot.get("point")), "%" + location.toLowerCase() + "%")
                        );

                return cb.or(
                        cb.like(cb.lower(root.get("origin")), "%" + location.toLowerCase() + "%"),
                        cb.like(cb.lower(root.get("destination")), "%" + location.toLowerCase() + "%"),
                        cb.exists(subquery)
                );
            };
        }

        // FIXED: Updated to use stopovers instead of stops
        static Specification<RideRequest> canAccommodateJourney(String origin, String destination) {
            return (root, query, cb) -> {
                // Subquery for origin in stopovers
                var originSubquery = query.subquery(Long.class);
                var originSubRoot = originSubquery.from(Stopover.class);
                originSubquery.select(cb.literal(1L))
                        .where(
                                cb.equal(originSubRoot.get("rideRequest"), root),
                                cb.like(cb.lower(originSubRoot.get("point")), "%" + origin.toLowerCase() + "%")
                        );

                // Subquery for destination in stopovers
                var destSubquery = query.subquery(Long.class);
                var destSubRoot = destSubquery.from(Stopover.class);
                destSubquery.select(cb.literal(1L))
                        .where(
                                cb.equal(destSubRoot.get("rideRequest"), root),
                                cb.like(cb.lower(destSubRoot.get("point")), "%" + destination.toLowerCase() + "%")
                        );

                return cb.and(
                        cb.or(
                                cb.like(cb.lower(root.get("origin")), "%" + origin.toLowerCase() + "%"),
                                cb.like(cb.lower(root.get("destination")), "%" + origin.toLowerCase() + "%"),
                                cb.exists(originSubquery)
                        ),
                        cb.or(
                                cb.like(cb.lower(root.get("origin")), "%" + destination.toLowerCase() + "%"),
                                cb.like(cb.lower(root.get("destination")), "%" + destination.toLowerCase() + "%"),
                                cb.exists(destSubquery)
                        )
                );
            };
        }

        static Specification<RideRequest> isOfferedRide() {
            return (root, query, cb) -> cb.equal(root.get("rideType"), "OFFERED");
        }

        static Specification<RideRequest> isRequestedRide() {
            return (root, query, cb) -> cb.equal(root.get("rideType"), "REQUESTED");
        }

        static Specification<RideRequest> hasCapacityFor(Integer passengerCount) {
            return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("vehicleCapacity"), passengerCount);
        }

        static Specification<RideRequest> isAfterCutoffTime(LocalDateTime dateTime) {
            return (root, query, cb) -> cb.greaterThan(root.get("travelDateTime"), dateTime);
        }

        static Specification<RideRequest> isPending() {
            return (root, query, cb) -> cb.equal(root.get("status"), "PENDING");
        }

        static Specification<RideRequest> isConfirmed() {
            return (root, query, cb) -> cb.equal(root.get("status"), "CONFIRMED");
        }

        static Specification<RideRequest> hasFemaleOnlyPreference() {
            return (root, query, cb) -> cb.equal(root.get("genderPreference"), "FEMALE_ONLY");
        }
    }
}