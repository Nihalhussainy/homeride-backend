package com.homeride.backend.dto;

import lombok.Data;

@Data
public class RatingDTO {
    private Long rideRequestId;
    private Long rateeId;
    private int score;
    private String comment;
}