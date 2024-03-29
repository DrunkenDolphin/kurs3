package com.sberbank.sweater.service;
import com.sberbank.sweater.Entities.Message;
import com.sberbank.sweater.Entities.Role;
import com.sberbank.sweater.Entities.User;
import com.sberbank.sweater.Repo.MessageRepository;
import com.sberbank.sweater.Repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private MailSender mailSender;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private MessageRepository messageRepository;
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username);
        if(user == null) {
            throw new UsernameNotFoundException("User not found");
        }
        return user;
    }

    public boolean addUser(User user) {
        User userFromDb = userRepository.findByUsername(user.getUsername());
        if (userFromDb != null) {
            return false;
        }
        user.setActive(true);
        user.setRoles(Collections.singleton(Role.USER));
        user.setActivationCode(UUID.randomUUID().toString());
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        userRepository.save(user);

        sendMessage(user);

        return true;
    }

    private void sendMessage(User user) {
        if (!user.getEmail().isEmpty()) {
            String message = String.format(
                    "Hello, %s! \n" +
                            "Welcome to Diary. Link to access your account: http://localhost:8080/activate/%s",
                    user.getUsername(),
                    user.getActivationCode()
            );
            mailSender.send(user.getEmail(), "Activation Code", message);
        }
    }


    public boolean activateUser(String code) {
       User user =  userRepository.findByActivationCode(code);
        if (user == null) {
            return false;
        }
        user.setActivationCode(null);
        userRepository.save(user);
        return true;
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public void saveUser(User user, String username, Map<String, String> form) {
        user.setUsername(username);

        Set<String> roles =  Arrays.stream(Role.values()).map(Role::name).collect(Collectors.toSet());
        user.getRoles().clear();
        for(String key : form.keySet()) {
            if(roles.contains(key)){
                user.getRoles().add(Role.valueOf(key));
            }
        }

        userRepository.save(user);
    }

    public void updateProfile(User user, String password, String email) {
        String dbEmail = user.getEmail();
        boolean isEmailChanged = (email != null && !email.equals(dbEmail)) ||
                (dbEmail != null && !dbEmail.equals(email));
        if(isEmailChanged) {
            user.setEmail(email);
            if(!StringUtils.isEmpty(email)) {
                user.setActivationCode(UUID.randomUUID().toString());
            }
        }
        if(!StringUtils.isEmpty(password)) {
            user.setPassword(passwordEncoder.encode(password));
        }
        userRepository.save(user);
        if(isEmailChanged) {
            sendMessage(user);
        }
    }

    public void subscribe(User currentUser, User user) {
        user.getSubscribers().add(currentUser);

        userRepository.save(user);
    }

    public void unsubscribe(User currentUser, User user) {
        user.getSubscribers().remove(currentUser);

        userRepository.save(user);
    }

    public Page<Message> messageListForUser(Pageable pageable, User currentUser, User user) {
        return messageRepository.findByUser(pageable, user);
    }
}
