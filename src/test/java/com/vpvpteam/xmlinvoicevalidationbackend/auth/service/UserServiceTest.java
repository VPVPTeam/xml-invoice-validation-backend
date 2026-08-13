package com.vpvpteam.xmlinvoicevalidationbackend.auth.service;

import com.vpvpteam.xmlinvoicevalidationbackend.auth.dto.CreateUserRequest;
import com.vpvpteam.xmlinvoicevalidationbackend.auth.entity.UserEntity;
import com.vpvpteam.xmlinvoicevalidationbackend.auth.enums.Role;
import com.vpvpteam.xmlinvoicevalidationbackend.auth.mapper.UserMapper;
import com.vpvpteam.xmlinvoicevalidationbackend.auth.model.User;
import com.vpvpteam.xmlinvoicevalidationbackend.auth.repository.UserRepository;
import com.vpvpteam.xmlinvoicevalidationbackend.exceptions.EntityAlreadyExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final String EMAIL = "user@vpvpteam.com";
    private static final String RAW_PASSWORD = "secret-password";
    private static final String PASSWORD_HASH = "$2a$10$hashed";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Captor
    private ArgumentCaptor<UserEntity> savedUserCaptor;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, new UserMapper(), passwordEncoder);
    }

    @Test
    void createUser_withFreeEmail_storesHashInsteadOfRawPassword() {
        givenEmailIsFree();
        givenPasswordIsEncoded();
        givenRepositorySavesAsIs();

        userService.createUser(new CreateUserRequest(EMAIL, RAW_PASSWORD, Role.USER));

        verify(userRepository).save(savedUserCaptor.capture());
        UserEntity savedUser = savedUserCaptor.getValue();

        assertThat(savedUser.getPasswordHash()).isEqualTo(PASSWORD_HASH);
        assertThat(savedUser.getPasswordHash()).isNotEqualTo(RAW_PASSWORD);
        assertThat(savedUser.getEmail()).isEqualTo(EMAIL);
        assertThat(savedUser.getRole()).isEqualTo(Role.USER);
        assertThat(savedUser.isActive()).isTrue();
    }

    @Test
    void createUser_withFreeEmail_returnsModelOfSavedUser() {
        givenEmailIsFree();
        givenPasswordIsEncoded();
        givenRepositorySavesAsIs();

        User user = userService.createUser(new CreateUserRequest(EMAIL, RAW_PASSWORD, Role.ADMIN));

        assertThat(user.getId()).isEqualTo(42L);
        assertThat(user.getEmail()).isEqualTo(EMAIL);
        assertThat(user.getRole()).isEqualTo(Role.ADMIN);
        assertThat(user.isActive()).isTrue();
    }

    @Test
    void createUser_withTakenEmail_throwsConflict() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(new CreateUserRequest(EMAIL, RAW_PASSWORD, Role.USER)))
                .isInstanceOf(EntityAlreadyExistsException.class)
                .hasMessage("User already exists: " + EMAIL);
    }

    @Test
    void createUser_withTakenEmail_neitherEncodesNorSaves() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(new CreateUserRequest(EMAIL, RAW_PASSWORD, Role.USER)))
                .isInstanceOf(EntityAlreadyExistsException.class);

        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
    }

    private void givenEmailIsFree() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
    }

    private void givenPasswordIsEncoded() {
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(PASSWORD_HASH);
    }

    private void givenRepositorySavesAsIs() {
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity userToSave = invocation.getArgument(0);
            userToSave.setId(42L);
            return userToSave;
        });
    }
}