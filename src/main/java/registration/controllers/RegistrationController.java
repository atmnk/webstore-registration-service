package registration.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysql.jdbc.MysqlDataTruncation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import registration.dto.RegistrationDTO;
import registration.dto.UserDTO;
import registration.exceptions.InputError;
import registration.model.Role;
import registration.model.User;
import registration.repositories.RoleRepository;
import registration.repositories.UserRepository;
import registration.security.AccountCredentials;
import registration.security.TokenAuthenticationService;
import registration.service.UserManager;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.IOException;
import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;


@RestController
@RequestMapping(path="/auth")
public class RegistrationController {
    @Autowired
    private UserManager userManager;

    @Autowired
    private RoleRepository roleRepository;

    @PostMapping(path = "/register",consumes =
            {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE},
            produces={MediaType.APPLICATION_JSON_VALUE,MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    ResponseEntity<UserDTO> register(@Valid @RequestBody RegistrationDTO registrationDTO, Errors errors) throws InputError {
        if(errors.hasErrors()){
            throw new InputError(InputError.VALIDATION,errors.getAllErrors().get(0).getDefaultMessage());
        }
        User user=new User();
        user.setName(registrationDTO.getName());
        user.setEmail(registrationDTO.getEmail());
        user.setPassword(registrationDTO.getPassword());
        Role role_user = roleRepository.findByName("ROLE_USER");
        user.setRoles(Arrays.asList(role_user));
        UserDTO userDTO=new UserDTO(userManager.createUser(user));
        HttpHeaders responseHeaders=new HttpHeaders();
        responseHeaders.set("authorization","Bearer "+ TokenAuthenticationService.getToken(user));
        ResponseEntity<UserDTO> userDTOResponseEntity=new ResponseEntity<UserDTO>(userDTO,responseHeaders,HttpStatus.CREATED);
        return userDTOResponseEntity;
    }

    @GetMapping(path="/all")
    public @ResponseBody Iterable<UserDTO> getAllUsers() {
        return StreamSupport.stream(userManager.getAllUsers().spliterator(),false).map(UserDTO::new).collect(Collectors.toList());
    }

    @GetMapping(path="/roles")
    public @ResponseBody List<String> roles(@RequestHeader("Authorization") String authHeader) {
        return TokenAuthenticationService.getRoles(authHeader);
    }

    @ExceptionHandler(InputError.class)
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public @ResponseBody InputError handleException(InputError e) {
        return e;
    }
}