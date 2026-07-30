package com.CodeSphere.backend.service;
import com.CodeSphere.backend.model.UserNotification;
import java.util.List;
public interface NotificationService { void notify(Long userId,String title,String message); List<UserNotification> mine(Long userId); void markRead(Long userId,Long id); void markAllRead(Long userId); }
