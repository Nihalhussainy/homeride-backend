package com.homeride.backend.service;

import com.google.gson.*;
import com.homeride.backend.dto.ChatbotRequestDTO;
import com.homeride.backend.dto.ChatbotResponseDTO;
import com.homeride.backend.model.Employee;
import com.homeride.backend.model.RideRequest;
import com.homeride.backend.model.Stopover;
import com.homeride.backend.model.Rating;
import com.homeride.backend.repository.EmployeeRepository;
import com.homeride.backend.repository.RideRequestRepository;
import com.homeride.backend.repository.RatingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatbotService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private HttpClient httpClient;
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private RideRequestRepository rideRequestRepository;

    @Autowired
    private RatingRepository ratingRepository;

    @PostConstruct
    public void init() {
        System.out.println("DEBUG: API Key value = " + (apiKey == null ? "NULL" : apiKey.substring(0, Math.min(10, apiKey.length())) + "..."));
        if (apiKey == null || apiKey.trim().isEmpty() || apiKey.equals("YOUR_API_KEY")) {
            System.err.println("Gemini API Key is not configured properly in application.properties or environment variables.");
            return;
        }
        this.httpClient = HttpClient.newHttpClient();
        System.out.println("Chatbot Service Initialized Successfully with ULTRA-SMART context.");
    }

    public ChatbotResponseDTO generateResponse(ChatbotRequestDTO request) {
        String userMessage = request.getMessage().trim();
        String userEmail = request.getUserEmail();
        String reply;

        System.out.println("DEBUG: Received message: " + userMessage);
        System.out.println("DEBUG: User email: " + userEmail);

        if (httpClient == null) {
            System.err.println("Chatbot service not initialized. Check API Key and configuration.");
            return new ChatbotResponseDTO("Sorry, the AI model is not available right now. Please try again later.");
        }

        if (userEmail == null || userEmail.trim().isEmpty()) {
            System.err.println("ERROR: User email is null or empty");
            return new ChatbotResponseDTO("Unable to identify user. Please log in again.");
        }

        // CHECK FOR SUPPORT REQUEST FIRST - redirect to contact page
        if (isSupportRequest(userMessage)) {
            return new ChatbotResponseDTO(
                    "I'd be happy to help you get in touch with our support team! 📧\n\n" +
                            "Please visit our Contact Page where you can send us a message directly. " +
                            "Our team will get back to you within 24 hours.\n\n" +
                            "You can also email us at: contacthomeride@gmail.com\n\n" +
                            "Is there anything specific about your rides or account that I can help you with in the meantime?"
            );
        }

        try {
            Employee user = employeeRepository.findByEmail(userEmail).orElse(null);

            // Determine question type and build appropriate context
            QuestionType questionType = analyzeQuestion(userMessage);
            String userContext = buildContextBasedOnQuestionType(user, userEmail, questionType);

            JsonObject requestBody = buildRequest(userMessage, userContext, questionType);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(GEMINI_API_URL + "?key=" + apiKey))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonObject responseBody = JsonParser.parseString(response.body()).getAsJsonObject();
                reply = extractReply(responseBody);
            } else {
                System.err.println("Gemini API error. Status: " + response.statusCode() + " Body: " + response.body());
                reply = "Sorry, there was an issue connecting to the AI service. Please try again later.";
            }

        } catch (Exception e) {
            System.err.println("Error calling Gemini API: " + e.getMessage());
            e.printStackTrace();
            reply = "Sorry, I encountered an issue while processing your request. Please try asking differently.";
        }

        return new ChatbotResponseDTO(reply);
    }

    // Enum for question types
    enum QuestionType {
        GENERAL_KNOWLEDGE,      // General questions (geography, facts, etc.)
        RIDE_RELATED,           // Questions about user's rides
        ACCOUNT_RELATED,        // Questions about profile, credit, ratings
        FEATURE_RELATED,        // Questions about app features
        AMBIGUOUS               // Mixed or unclear
    }

    private QuestionType analyzeQuestion(String userMessage) {
        String message = userMessage.toLowerCase();

        // General knowledge keywords
        if (message.contains("distance between") && !message.contains("my ride") && !message.contains("my journey")) {
            return QuestionType.GENERAL_KNOWLEDGE;
        }
        if (message.matches(".*\\b(what is|what are|how|why|tell me|explain)\\b.*") &&
                !message.contains("ride") && !message.contains("booking") && !message.contains("my")) {
            return QuestionType.GENERAL_KNOWLEDGE;
        }

        // Ride-related keywords
        if (message.contains("ride") || message.contains("booking") || message.contains("journey") ||
                message.contains("driver") || message.contains("passenger") || message.contains("route")) {
            return QuestionType.RIDE_RELATED;
        }

        // Account-related keywords
        if (message.contains("credit") || message.contains("rating") || message.contains("profile") ||
                message.contains("account") || message.contains("history")) {
            return QuestionType.ACCOUNT_RELATED;
        }

        // Feature-related keywords
        if (message.contains("how do i") || message.contains("how to") || message.contains("can i") ||
                message.contains("feature") || message.contains("help")) {
            return QuestionType.FEATURE_RELATED;
        }

        return QuestionType.AMBIGUOUS;
    }

    private String buildContextBasedOnQuestionType(Employee user, String userEmail, QuestionType questionType) {
        switch (questionType) {
            case GENERAL_KNOWLEDGE:
                // Minimal context for general questions
                return buildMinimalUserContext(user);

            case RIDE_RELATED:
                // Full ride context
                return buildComprehensiveUserContext(user, userEmail);

            case ACCOUNT_RELATED:
                // Account and stats context only
                return buildAccountContext(user, userEmail);

            case FEATURE_RELATED:
                // Minimal context, focus on help
                return buildMinimalUserContext(user);

            case AMBIGUOUS:
            default:
                // Include relevant parts
                return buildComprehensiveUserContext(user, userEmail);
        }
    }

    private String buildMinimalUserContext(Employee user) {
        StringBuilder context = new StringBuilder();
        if (user != null) {
            context.append("=== USER PROFILE ===\n");
            context.append("Name: ").append(user.getName()).append("\n");
            context.append("Member Since: ").append(user.getCreatedAt()).append("\n\n");
        }
        return context.toString();
    }

    private String buildAccountContext(Employee user, String userEmail) {
        StringBuilder context = new StringBuilder();

        if (user != null) {
            context.append("=== USER PROFILE ===\n");
            context.append("Name: ").append(user.getName()).append("\n");
            context.append("Email: ").append(user.getEmail()).append("\n");
            context.append("Gender: ").append(user.getGender() != null ? user.getGender() : "Not specified").append("\n");
            context.append("Phone: ").append(user.getPhoneNumber() != null ? user.getPhoneNumber() : "Not provided").append("\n");

            // Average rating
            List<Rating> receivedRatings = ratingRepository.findByRateeId(user.getId());
            Double avgRating = null;
            if (!receivedRatings.isEmpty()) {
                avgRating = receivedRatings.stream().mapToInt(Rating::getScore).average().orElse(0.0);
                context.append("Average Rating: ").append(String.format("%.1f/5.0", avgRating)).append(" (").append(receivedRatings.size()).append(" ratings)\n");
            } else {
                context.append("Average Rating: No ratings yet\n");
            }

            context.append("Travel Credit: ₹").append(String.format("%.2f", user.getTravelCredit())).append("\n");
            context.append("Member Since: ").append(user.getCreatedAt()).append("\n\n");

            // Ride stats
            Long userId = user.getId();
            List<RideRequest> allRides = rideRequestRepository.findAll().stream()
                    .filter(r -> {
                        boolean isRequester = r.getRequester() != null && r.getRequester().getId().equals(userId);
                        boolean isParticipant = r.getParticipants() != null && r.getParticipants().stream()
                                .anyMatch(p -> p.getParticipant().getId().equals(userId));
                        return isRequester || isParticipant;
                    })
                    .collect(Collectors.toList());

            List<RideRequest> pastRides = allRides.stream()
                    .filter(r -> r.getTravelDateTime().isBefore(LocalDateTime.now()))
                    .collect(Collectors.toList());

            context.append("=== RIDE HISTORY ===\n");
            context.append("Total Rides: ").append(allRides.size()).append("\n");
            context.append("Completed Rides: ").append(pastRides.size()).append("\n");
            long offeredCount = allRides.stream().filter(r -> r.getRequester().equals(user)).count();
            long joinedCount = allRides.stream().filter(r -> !r.getRequester().equals(user)).count();
            context.append("Rides Offered: ").append(offeredCount).append("\n");
            context.append("Rides Joined: ").append(joinedCount).append("\n");
        }

        return context.toString();
    }

    private String buildComprehensiveUserContext(Employee user, String userEmail) {
        StringBuilder context = new StringBuilder();

        if (user != null) {
            context.append("=== USER PROFILE ===\n");
            context.append("Name: ").append(user.getName()).append("\n");
            context.append("Email: ").append(user.getEmail()).append("\n");
            context.append("Gender: ").append(user.getGender() != null ? user.getGender() : "Not specified").append("\n");
            context.append("Phone: ").append(user.getPhoneNumber() != null ? user.getPhoneNumber() : "Not provided").append("\n");

            // Calculate and include average rating
            List<Rating> receivedRatings = ratingRepository.findByRateeId(user.getId());
            Double avgRating = null;
            if (!receivedRatings.isEmpty()) {
                avgRating = receivedRatings.stream().mapToInt(Rating::getScore).average().orElse(0.0);
                context.append("Average Rating: ").append(String.format("%.1f/5.0", avgRating)).append(" (").append(receivedRatings.size()).append(" ratings)\n");
            } else {
                context.append("Average Rating: No ratings yet\n");
            }

            context.append("Travel Credit: ₹").append(String.format("%.2f", user.getTravelCredit())).append("\n");
            context.append("Member Since: ").append(user.getCreatedAt()).append("\n\n");

            Long userId = user.getId();

            // Get ALL rides, filter for UPCOMING only
            List<RideRequest> allRides = rideRequestRepository.findAll().stream()
                    .filter(r -> {
                        boolean isRequester = r.getRequester() != null && r.getRequester().getId().equals(userId);
                        boolean isParticipant = r.getParticipants() != null && r.getParticipants().stream()
                                .anyMatch(p -> p.getParticipant().getId().equals(userId));
                        return isRequester || isParticipant;
                    })
                    .filter(r -> r.getTravelDateTime().isAfter(LocalDateTime.now())) // ONLY FUTURE RIDES
                    .sorted((a, b) -> a.getTravelDateTime().compareTo(b.getTravelDateTime()))
                    .collect(Collectors.toList());

            System.out.println("DEBUG: Upcoming rides only: " + allRides.size());

            // Separate upcoming and past rides
            List<RideRequest> upcomingRides = allRides.stream()
                    .filter(r -> r.getTravelDateTime().isAfter(LocalDateTime.now()))
                    .collect(Collectors.toList());

            List<RideRequest> pastRides = allRides.stream()
                    .filter(r -> r.getTravelDateTime().isBefore(LocalDateTime.now()))
                    .collect(Collectors.toList());

            // UPCOMING RIDES
            context.append("=== UPCOMING RIDES (").append(upcomingRides.size()).append(") ===\n");
            if (!upcomingRides.isEmpty()) {
                for (int i = 0; i < Math.min(5, upcomingRides.size()); i++) {
                    RideRequest ride = upcomingRides.get(i);
                    context.append("\n[Ride ").append(i + 1).append("]\n");
                    context.append("  Route: ").append(ride.getOriginCity()).append(" → ").append(ride.getDestinationCity()).append("\n");
                    context.append("  Date/Time: ").append(formatDateTime(ride.getTravelDateTime())).append("\n");
                    context.append("  Role: ").append(ride.getRequester().equals(user) ? "DRIVER" : "PASSENGER").append("\n");
                    context.append("  Price: ₹").append(ride.getPrice() != null ? String.format("%.2f", ride.getPrice()) : "Not set").append("\n");
                    context.append("  Distance: ").append(ride.getDistance() != null ? String.format("%.1f km", ride.getDistance()) : "N/A").append("\n");
                    context.append("  Duration: ").append(ride.getDuration() != null ? ride.getDuration() + " mins" : "N/A").append("\n");

                    if (ride.getRideType().equals("OFFERED")) {
                        context.append("  Vehicle: ").append(ride.getVehicleModel()).append(" (").append(ride.getVehicleCapacity()).append(" total seats)\n");
                        int bookedSeats = ride.getParticipants() != null ?
                                (int)ride.getParticipants().stream().mapToInt(p -> p.getNumberOfSeats() != null ? p.getNumberOfSeats() : 1).sum() : 0;
                        context.append("  Available: ").append(ride.getVehicleCapacity() - bookedSeats).append(" seats\n");
                        context.append("  Gender Preference: ").append(ride.getGenderPreference() != null ? ride.getGenderPreference() : "Any").append("\n");
                        if (ride.getDriverNote() != null && !ride.getDriverNote().isEmpty()) {
                            context.append("  Driver Note: ").append(ride.getDriverNote()).append("\n");
                        }
                    }

                    if (ride.getStopovers() != null && !ride.getStopovers().isEmpty()) {
                        context.append("  Stopovers: ");
                        String stops = ride.getStopovers().stream()
                                .map(Stopover::getCity)
                                .collect(Collectors.joining(" → "));
                        context.append(stops).append("\n");
                    }

                    context.append("  Driver: ").append(ride.getRequester().getName());
                    Double driverRating = calculateAvgRating(ride.getRequester().getId());
                    context.append(" (Rating: ").append(driverRating != null ? String.format("%.1f", driverRating) : "N/A").append(")\n");
                }
            } else {
                context.append("No upcoming rides. Time to book or offer one!\n");
            }

            // RIDE HISTORY STATS
            context.append("\n=== RIDE HISTORY ===\n");
            context.append("Total Rides: ").append(allRides.size()).append("\n");
            context.append("Completed Rides: ").append(pastRides.size()).append("\n");
            long offeredCount = allRides.stream().filter(r -> r.getRequester().equals(user)).count();
            long joinedCount = allRides.stream().filter(r -> !r.getRequester().equals(user)).count();
            context.append("Rides Offered: ").append(offeredCount).append("\n");
            context.append("Rides Joined: ").append(joinedCount).append("\n");

        } else {
            context.append("User not found for email: ").append(userEmail).append("\n");
        }

        return context.toString();
    }

    private Double calculateAvgRating(Long employeeId) {
        List<Rating> ratings = ratingRepository.findByRateeId(employeeId);
        if (ratings == null || ratings.isEmpty()) {
            return null;
        }
        return ratings.stream().mapToDouble(Rating::getScore).average().orElse(0.0);
    }

    private String formatDateTime(LocalDateTime dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' HH:mm");
        return dateTime.format(formatter);
    }

    private JsonObject buildRequest(String userMessage, String userContext, QuestionType questionType) {
        String systemPrompt = buildSystemPrompt(userContext, questionType);

        JsonObject requestBody = new JsonObject();
        JsonArray contents = new JsonArray();

        JsonObject content = new JsonObject();
        content.addProperty("role", "user");

        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();
        part.addProperty("text", systemPrompt + "\n\nUser message: " + userMessage);
        parts.add(part);

        content.add("parts", parts);
        contents.add(content);

        requestBody.add("contents", contents);

        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("temperature", 0.85);
        generationConfig.addProperty("maxOutputTokens", 1000);
        requestBody.add("generationConfig", generationConfig);

        return requestBody;
    }

    private String buildSystemPrompt(String userContext, QuestionType questionType) {
        String basePrompt = "You are GOGO, the ultra-smart AI assistant for HomeRide. HomeRide was created by Nihal Hussain, an intern at Sopra Steria. " +
                "You have access to relevant user data below when applicable.\n\n" +
                userContext + "\n\n";

        String typeSpecificRules = "";

        switch (questionType) {
            case GENERAL_KNOWLEDGE:
                typeSpecificRules = "GENERAL KNOWLEDGE QUESTION:\n" +
                        "1. Answer factual questions directly and accurately\n" +
                        "2. Use your general knowledge - not limited to HomeRide data\n" +
                        "3. For questions like 'distance between cities', provide accurate geographic information\n" +
                        "4. Be clear, concise, and informative\n" +
                        "5. If the question is about HomeRide features, mention it's a ride-sharing app\n";
                break;

            case RIDE_RELATED:
                typeSpecificRules = "RIDE-RELATED QUESTION:\n" +
                        "1. Use EXACT data from user's rides: prices in ₹, distances in km, times in HH:mm\n" +
                        "2. When route is mentioned (e.g., 'Nellore to Gudur'), identify that specific ride\n" +
                        "3. Provide complete journey details: origin, destination, stopovers, distance, duration, driver name & rating\n" +
                        "4. Reference upcoming rides when relevant\n" +
                        "5. Be specific with numbers - don't round or estimate\n";
                break;

            case ACCOUNT_RELATED:
                typeSpecificRules = "ACCOUNT-RELATED QUESTION:\n" +
                        "1. Reference user's rating, travel credit, and ride history\n" +
                        "2. Provide specific numbers from their profile\n" +
                        "3. Highlight achievements (e.g., 'You've completed X rides')\n" +
                        "4. Be encouraging about their HomeRide journey\n";
                break;

            case FEATURE_RELATED:
                typeSpecificRules = "FEATURE/HELP QUESTION:\n" +
                        "1. Explain HomeRide features clearly and step-by-step\n" +
                        "2. Use simple language for how-to questions\n" +
                        "3. Reference their specific situation if relevant\n" +
                        "4. Be helpful and encouraging\n";
                break;

            case AMBIGUOUS:
            default:
                typeSpecificRules = "MIXED/AMBIGUOUS QUESTION:\n" +
                        "1. Address all aspects of the question\n" +
                        "2. Use ride data when relevant, general knowledge when needed\n" +
                        "3. Be comprehensive but concise\n";
                break;
        }

        String commonRules = "\n\nCOMMON RULES:\n" +
                "1. Remember all context from this conversation - avoid repeating yourself\n" +
                "2. Be warm, witty, and genuinely helpful with occasional light humor\n" +
                "3. If you don't have information about something, say so clearly\n" +
                "4. Suggest relevant features based on their history when appropriate\n" +
                "5. Keep responses concise but informative\n";

        return basePrompt + typeSpecificRules + commonRules;
    }
    private boolean isSupportRequest(String userMessage) {
        String message = userMessage.toLowerCase();

        return message.contains("customer care") ||
                message.contains("customer servic") ||
                message.contains("customer support") ||
                message.contains("support") ||
                message.contains("help") && (message.contains("contact") || message.contains("reach")) ||
                message.contains("complaint") ||
                message.contains("issue") && message.contains("help") ||
                message.contains("contact us") ||
                message.contains("reach out") ||
                message.contains("talk to") && message.contains("support");
    }

    private String extractReply(JsonObject responseBody) {
        if (responseBody.has("candidates") && responseBody.getAsJsonArray("candidates").size() > 0) {
            JsonObject candidate = responseBody.getAsJsonArray("candidates").get(0).getAsJsonObject();

            if (candidate.has("content") && candidate.getAsJsonObject("content").has("parts")) {
                JsonArray parts = candidate.getAsJsonObject("content").getAsJsonArray("parts");
                if (parts.size() > 0) {
                    JsonObject textPart = parts.get(0).getAsJsonObject();
                    if (textPart.has("text")) {
                        return textPart.get("text").getAsString();
                    }
                }
            }
        }

        return "Sorry, I couldn't generate a response right now.";
    }
}