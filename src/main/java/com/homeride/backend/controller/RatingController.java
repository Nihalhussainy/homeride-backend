package com.homeride.backend.controller;

import com.homeride.backend.dto.RatingDTO;
import com.homeride.backend.model.Rating;
import com.homeride.backend.service.RatingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/ratings")
public class RatingController {

    private final RatingService ratingService;

    @Autowired
    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    @PostMapping
    public ResponseEntity<Rating> submitRating(@RequestBody RatingDTO ratingDTO, Principal principal) {
        String raterEmail = principal.getName();
        Rating newRating = ratingService.submitRating(ratingDTO, raterEmail);
        return ResponseEntity.ok(newRating);
    }

    @GetMapping("/my-ratings")
    public ResponseEntity<List<Rating>> getMyRatings(Principal principal) {
        List<Rating> ratings = ratingService.getRatingsForUser(principal.getName());
        return ResponseEntity.ok(ratings);
    }

    @GetMapping("/given")
    public ResponseEntity<List<Rating>> getGivenRatings(Principal principal) {
        List<Rating> ratings = ratingService.getRatingsGivenByUser(principal.getName());
        return ResponseEntity.ok(ratings);
    }
}