package com.pulseconnect.repository;

import com.pulseconnect.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);
    
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);
    
       Optional<User> findByPhoneNumber(String phoneNumber);
    
    List<User> findByRole(User.UserRole role);
    
    List<User> findByStatus(User.UserStatus status);
    
    List<User> findByBloodGroup(User.BloodGroup bloodGroup);
    
    List<User> findByCityAndBloodGroup(String city, User.BloodGroup bloodGroup);
    
    @Query("SELECT u FROM User u WHERE u.city = :city AND u.role = :role")
    List<User> findByCityAndRole(@Param("city") String city, @Param("role") User.UserRole role);
    
    @Query("SELECT u FROM User u WHERE " +
           "(6371 * acos(cos(radians(:latitude)) * cos(radians(u.latitude)) * " +
           "cos(radians(u.longitude) - radians(:longitude)) + sin(radians(:latitude)) * " +
           "sin(radians(u.latitude)))) <= :radiusKm")
    List<User> findUsersWithinRadius(@Param("latitude") Double latitude, 
                                      @Param("longitude") Double longitude, 
                                      @Param("radiusKm") Double radiusKm);
}
