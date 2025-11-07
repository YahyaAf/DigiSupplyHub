package org.project.digital_logistics.service;

import jakarta.servlet.http.HttpSession;
import org.project.digital_logistics.model.enums.Role;
import org.project.digital_logistics.exception.AccessDeniedException;
import org.project.digital_logistics.model.User;
import org.project.digital_logistics.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PermissionService {

    private static final String SESSION_USER_KEY = "authenticated_user";

    private final UserRepository userRepository;

    @Autowired
    public PermissionService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // ✅ Get authenticated user
    public User getAuthenticatedUser(HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_KEY);
        if (userId == null) {
            throw new IllegalStateException("Not authenticated. Please login.");
        }

        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));
    }

    // ✅ Check if user is ADMIN
    public void requireAdmin(HttpSession session) {
        User user = getAuthenticatedUser(session);
        if (user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Access denied. Admin privileges required.");
        }
    }

    // ✅ Check if user is ADMIN or WAREHOUSE_MANAGER
    public void requireWarehouseManager(HttpSession session) {
        User user = getAuthenticatedUser(session);
        if (user.getRole() != Role.ADMIN && user.getRole() != Role.WAREHOUSE_MANAGER) {
            throw new AccessDeniedException("Access denied. Warehouse Manager or Admin privileges required.");
        }
    }

    // ✅ Check if CLIENT owns the resource
    public void checkClientOwnership(HttpSession session, Long resourceClientId) {
        User user = getAuthenticatedUser(session);

        // ADMIN & WAREHOUSE_MANAGER can access everything
        if (user.getRole() == Role.ADMIN || user.getRole() == Role.WAREHOUSE_MANAGER) {
            return;
        }

        // CLIENT can only access own resources
        if (user.getRole() == Role.CLIENT && !user.getId().equals(resourceClientId)) {
            throw new AccessDeniedException("Access denied. You can only access your own orders.");
        }
    }
}