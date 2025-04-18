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

@Controller
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public String profile(Principal principal, Model model) {
        String currentUsername = principal.getName();

        // 현재 사용자의 아이디로 사용자 정보를 조회
        User user = userService.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        // 조회한 정보를 모델에 추가
        model.addAttribute("user", user);

        return "users/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@ModelAttribute User user, Principal principal, RedirectAttributes redirectAttributes) {
        try {
            userService.updateUser(user, principal);
            redirectAttributes.addFlashAttribute("successMessage", "프로필이 성공적으로 수정되었습니다.");
            return "redirect:/users/profile";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/users/profile";
        }
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
        
        try {
            // 사용자 상태 초기화
            user.setStatus(User.Status.ACTIVE);
            user.setEnabled(true);
            user.setLoginAttempts(0);
            user.setAccountLocked(false);
            user.setLastLoginAt(LocalDateTime.now());

            // 사용자 등록
            userService.register(user);
            redirectAttributes.addFlashAttribute("successMessage", "회원가입이 완료되었습니다. 로그인해주세요.");
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            result.rejectValue("username", "error.user", e.getMessage());
            return "user/register";
        }
    }

    @GetMapping("/edit")
    public String editProfile(Principal principal, Model model) {
        User user = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
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
        
        User existingUser = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        // 기존 사용자 정보 업데이트
        existingUser.setName(user.getName());
        existingUser.setEmail(user.getEmail());
        existingUser.setPhone(user.getPhone());
        existingUser.setAddress(user.getAddress());
        
        // 비밀번호가 입력된 경우에만 업데이트
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            existingUser.setPassword(user.getPassword());
        }
        
        userService.save(existingUser);
        redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully");
        return "redirect:/users/profile";
    }
} 