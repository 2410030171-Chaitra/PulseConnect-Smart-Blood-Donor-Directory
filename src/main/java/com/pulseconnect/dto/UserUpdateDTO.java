package com.pulseconnect.dto;

import com.pulseconnect.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateDTO {
    private String fullName;
    private String phoneNumber;
    private User.BloodGroup bloodGroup;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private String preferredLanguage;
}
