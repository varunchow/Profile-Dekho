package com.profiledekho.app.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlatformStats {
    private String platform; // "codeforces", "leetcode", etc.
    private Boolean valid; // Was the fetch successful?
    
    // Common fields
    private Integer solved;
    private Integer rating;
    private Integer maxRating;
    private String rankName;
    private Integer contests;
    
    // Platform-specific
    private Integer easy;
    private Integer medium;
    private Integer hard;
    private Integer stars;
    private Integer codingScore;
    private Integer ranking;
    private Integer globalRanking;
    
    // InterviewBit-specific
    private Boolean private_;
    
    // Error/status messages
    private String error;
    private String message;
}
