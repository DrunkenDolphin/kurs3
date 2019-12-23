package com.sberbank.sweater.controllers;

import com.sberbank.sweater.Entities.User;
import com.sberbank.sweater.Entities.dto.CaptchaResponseDto;
import com.sberbank.sweater.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import javax.persistence.Transient;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

@Controller
public class RegistrationController {

    private static final String CAPTCHA_URL = "https://www.google.com/recaptcha/api/siteverify?secret=%s&response=%s";
    @Autowired
    private UserService userService;
    @Value("${recaptcha.secret}")
    private String recaptcha;
    @Autowired
    private RestTemplate restTemplate;


    @GetMapping("/registration")
    public String registration() {
        return "registration";
    }

    @PostMapping("/registration")
    public String addUser(
            @RequestParam("password2") String password2,
            @RequestParam("g-recaptcha-response") String captchResponse,
            @Valid User user,
            BindingResult bindingResult,
            Model model) {
        String url = String.format(CAPTCHA_URL, recaptcha, captchResponse);
       CaptchaResponseDto captchaResponseDto = restTemplate.postForObject(url, Collections.emptyList(), CaptchaResponseDto.class);

       if(!captchaResponseDto.isSuccess()) {
           model.addAttribute("captchaError", "fill captcha");
       }

        boolean isPassword2Empty = StringUtils.isEmpty(password2);
        if(isPassword2Empty) {
            model.addAttribute("password2Error","password conf cannot be empty");
        }
        if(user.getPassword() != null && !user.getPassword().equals(password2)) {
            model.addAttribute("passwordError", "Passwords are different");
        }

        if(isPassword2Empty || bindingResult.hasErrors() || !captchaResponseDto.isSuccess()) {
            Map<String, String> errors = ControllersUtils.getErrors(bindingResult);
            model.mergeAttributes(errors);
            return "registration";
        }
        if (!userService.addUser(user)) {
            model.addAttribute("usernameError", "User exists!");
            return "registration";
        }

        return "redirect:/login";
    }

    @GetMapping("/activate/{code}")
    public String activate(Model model, @PathVariable String code) {
        boolean isActivated = userService.activateUser(code);

        if(isActivated) {
            model.addAttribute("messageType","success");
            model.addAttribute("message","User activated");
        }
        else {
            model.addAttribute("messageType","danger");
            model.addAttribute("message", "Activation code not found");
        }
        return "login";
    }
}