package com.jacob.testapp.user.controller;

import com.jacob.testapp.user.entity.User;
import com.jacob.testapp.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.function.Supplier;

@Controller
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public String profile(Principal principal, Model model) {
        User user = getCurrentUser(principal);
        model.addAttribute("user", user);
        return "users/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@ModelAttribute User user, Principal principal, RedirectAttributes redirectAttributes) {
        return handleUserOperation(() -> {
            userService.updateUser(user, principal);
            return "프로필이 성공적으로 수정되었습니다.";
        }, redirectAttributes, "/users/profile");
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("user", new User());
        return "user/register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("user") User user, 
                              BindingResult result,
                              @RequestParam(value = "confirmPassword", required = false) String confirmPassword,
                              RedirectAttributes redirectAttributes) {
        // 비밀번호 확인 검증
        if (confirmPassword == null || !confirmPassword.equals(user.getPassword())) {
            result.rejectValue("password", "error.user", "비밀번호와 비밀번호 확인이 일치하지 않습니다.");
        }

        if (result.hasErrors()) {
            return "user/register";
        }
        
        return handleUserOperation(() -> {
            initializeNewUser(user);
            userService.register(user);
            return "회원가입이 완료되었습니다. 로그인해주세요.";
        }, redirectAttributes, "/login", error -> {
            result.rejectValue("username", "error.user", error.getMessage());
            return "user/register";
        });
    }

    @GetMapping("/edit")
    public String editProfile(Principal principal, Model model) {
        User user = getCurrentUser(principal);
        model.addAttribute("user", user);
        return "user/edit";
    }

    @PostMapping("/edit")
    public String updateProfile(@Valid @ModelAttribute("user") User user, 
                               BindingResult result, 
                               Principal principal,
                               RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "user/edit";
        }
        
        return handleUserOperation(() -> {
            User existingUser = getCurrentUser(principal);
            updateUserFields(existingUser, user);
            userService.save(existingUser);
            return "Profile updated successfully";
        }, redirectAttributes, "/users/profile");
    }
    
    // ========== 헬퍼 메서드 ==========
    
    /**
     * 현재 로그인한 사용자 정보 조회
     */
    private User getCurrentUser(Principal principal) {
        return userService.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
    }
    
    /**
     * 새 사용자 초기화
     */
    private void initializeNewUser(User user) {
        user.setStatus(User.Status.ACTIVE);
        user.setEnabled(true);
        user.setLoginAttempts(0);
        user.setAccountLocked(false);
        user.setLastLoginAt(LocalDateTime.now());
    }
    
    /**
     * 사용자 필드 업데이트
     */
    private void updateUserFields(User existingUser, User updatedUser) {
        existingUser.setName(updatedUser.getName());
        existingUser.setEmail(updatedUser.getEmail());
        existingUser.setPhone(updatedUser.getPhone());
        existingUser.setAddress(updatedUser.getAddress());
        
        // 비밀번호가 입력된 경우에만 업데이트
        if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
            existingUser.setPassword(updatedUser.getPassword());
        }
    }
    
    /**
     * 사용자 작업 처리 및 리다이렉트
     */
    private String handleUserOperation(Supplier<String> operation, 
                                      RedirectAttributes redirectAttributes, 
                                      String redirectUrl) {
        return handleUserOperation(operation, redirectAttributes, redirectUrl, error -> {
            redirectAttributes.addFlashAttribute("errorMessage", error.getMessage());
            return "redirect:" + redirectUrl;
        });
    }
    
    /**
     * 사용자 작업 처리 및 리다이렉트 (에러 핸들러 포함)
     */
    private String handleUserOperation(Supplier<String> operation, 
                                      RedirectAttributes redirectAttributes, 
                                      String redirectUrl,
                                      java.util.function.Function<Exception, String> errorHandler) {
        try {
            String successMessage = operation.get();
            redirectAttributes.addFlashAttribute("successMessage", successMessage);
            return "redirect:" + redirectUrl;
        } catch (Exception e) {
            return errorHandler.apply(e);
        }
    }
} 