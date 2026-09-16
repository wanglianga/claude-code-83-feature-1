package com.community.haircut.service;

import com.community.haircut.common.BizException;
import com.community.haircut.entity.*;
import com.community.haircut.enums.Role;
import com.community.haircut.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final BarberProfileRepository barberProfileRepository;
    private final VolunteerProfileRepository volunteerProfileRepository;
    private final ToolKitRepository toolKitRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, BarberProfileRepository barberProfileRepository,
                       VolunteerProfileRepository volunteerProfileRepository, ToolKitRepository toolKitRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.barberProfileRepository = barberProfileRepository;
        this.volunteerProfileRepository = volunteerProfileRepository;
        this.toolKitRepository = toolKitRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> list() {
        return userRepository.findAll();
    }

    public List<User> listByRole(Role role) {
        return userRepository.findByRole(role);
    }

    @Transactional
    public User create(String username, String password, String realName, String phone, Role role) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new BizException("用户名已存在");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRealName(realName);
        user.setPhone(phone);
        user.setRole(role);
        User saved = userRepository.save(user);
        if (role == Role.BARBER) {
            BarberProfile profile = new BarberProfile();
            profile.setUserId(saved.getId());
            barberProfileRepository.save(profile);
            ToolKit kit = new ToolKit();
            kit.setBarberId(saved.getId());
            kit.setName("标准理发工具包");
            kit.setItems("剪刀,推子,梳子,围布,消毒喷雾,服务包");
            // 新理发师工具默认待消毒，消毒完成后才可被派单
            toolKitRepository.save(kit);
        } else if (role == Role.VOLUNTEER) {
            VolunteerProfile profile = new VolunteerProfile();
            profile.setUserId(saved.getId());
            volunteerProfileRepository.save(profile);
        }
        return saved;
    }

    @Transactional
    public void changeStatus(Long id, String status) {
        User user = userRepository.findById(id).orElseThrow(() -> new BizException("用户不存在"));
        user.setStatus(status);
        userRepository.save(user);
    }

    @Transactional
    public void resetPassword(Long id, String newPassword) {
        User user = userRepository.findById(id).orElseThrow(() -> new BizException("用户不存在"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
