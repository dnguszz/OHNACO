package com.prossafy101.ohnaco.controller;

import com.prossafy101.ohnaco.entity.user.*;
import com.prossafy101.ohnaco.repository.StatisticsRepository;
import com.prossafy101.ohnaco.service.*;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.view.RedirectView;
import reactor.netty.http.server.HttpServerRequest;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private StatisticsRepository stRepo;

    @Autowired
    private TodoService todoService;

    @Autowired
    private NotificationService notificationService;

    private final Logger logger = LoggerFactory.getLogger(this.getClass().getSimpleName());
    @GetMapping("/rss")
    public Object rss() {
        Map<String, Object> result = new HashMap<>();

        String rssUrl = "http://javacan.tistory.com/rss";

        try {
            URL feedUrl = new URL(rssUrl);
            SyndFeedInput input = new SyndFeedInput();
            SyndFeed feed = input.build(new XmlReader(feedUrl));

            System.out.println("RSS title: " + feed.getTitle());
            System.out.println("RSS author: " + feed.getAuthor());

            List entries = feed.getEntries();
            for (int i = 0; i < entries.size(); i++) {
                String[] arr = new String[5];
                SyndEntry entry = (SyndEntry) entries.get(i);
                System.out.println("--- Entry " + i);
                System.out.println(entry.getTitle());
                arr[0] = entry.getTitle();
                System.out.println(entry.getAuthor());
                arr[1] = entry.getAuthor();
                System.out.println(entry.getDescription().getValue());
                arr[2] = entry.getDescription().getValue();
                System.out.println(entry.getLink());
                arr[3] = entry.getLink();
                System.out.println(entry.getPublishedDate());
                arr[4] = entry.getPublishedDate().toString();
                result.put("a" + i , arr);
            }
        } catch (IllegalArgumentException e) {
            // ...
        } catch (FeedException e) {
            // ...
        } catch (IOException e) {
            // ...
        }

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @PostMapping("/test")
    public ResponseEntity<?> test() {
        Map<String, Object> result = new HashMap<>();
        System.out.println(LocalDateTime.of(LocalDate.parse("2021-07-25"), LocalTime.of(23,59,59)));
        Map<String, Object> map = new HashMap<>();
        map.put("startDate", LocalDateTime.of(LocalDate.parse("2021-07-01"), LocalTime.of(0,0,0)));
        map.put("endDate", LocalDateTime.of(LocalDate.parse("2021-07-25"), LocalTime.of(23,59,59)));
        map.put("userid", "LB34PP33DAE57");

        result.put("test", stRepo.getCategoryTime(map));
        return new ResponseEntity<>(result,HttpStatus.OK);
    }

    @GetMapping("/info")
    public Object getInfo(HttpServletRequest req) {
        String token = req.getHeader("Authorization").substring(7);
        String userid = jwtUtil.getUserid(token);

        Map<String, Object> result = new HashMap<>();
        result.put("user", userService.findByUserid(userid));

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @PostMapping("/signIn")
    public ResponseEntity<Map<String, Object>> signIn(@RequestBody SignInDto dto, HttpServletResponse res) {
        HttpStatus status = null;
        Map<String, Object> result = new HashMap<>();
        try {
            User user = userService.signIn(dto);

            String accessToken = jwtUtil.generateAccessToken(user.getUserid(), dto.getEmail());
            String refreshToken = jwtUtil.generateRefreshToken(user.getUserid(), dto.getEmail());

            logger.info(refreshToken);
            res.setHeader("Authorization", "Bearer " + accessToken);
            redisUtil.setData(user.getEmail(), refreshToken,  JwtUtil.REFRESH_TOKEN_VALIDATION_SECOND);

            result.put("userId", user.getUserid());
            result.put("nickname", user.getNickname());
            result.put("githubId", user.getGithubid());
            result.put("image", user.getImage());
            status = HttpStatus.OK;
        } catch (Exception e) {
            status = HttpStatus.UNAUTHORIZED;
            result.put("message", e.getMessage());
        }

        return new ResponseEntity<>(result , status);
    }

    @GetMapping("/signOut")
    public Object signOut(@RequestParam String email) {
        redisUtil.deleteData(email);

        notificationService.tokenMap.remove(email);
        return new ResponseEntity<>(HttpStatus.OK);
    }


    @GetMapping("/join/idcheck")
    @ApiOperation(value = "????????? ?????? ??????")
    public Object checkId(@RequestParam(required = true) String email) {
        Map<String, Object> result = new HashMap<>();
        try {
            userService.isSameEmail(email);
            result.put("status", true);
            result.put("message", "????????? ???????????? ????????????.");
        } catch (Exception e) {
            result.put("status", false);
            result.put("message", e.getMessage());
        }
        //????????? ????????? ????????? ?????????key ??????!!!! ????????? ????????????
//        userService.tempUserDelete(email);
        return new ResponseEntity<>(result , HttpStatus.OK);
    }

    @GetMapping("/namecheck")
    @ApiOperation(value = "????????? ?????? ??????")
    public Object checkNickname(@RequestParam(required = true) String nickname) {
        Map<String, Object> result = new HashMap<>();

        try {
            userService.isSameNickname(nickname);
            result.put("status", true);
            result.put("message", "????????? ???????????? ????????????.");
        } catch (Exception e) {
            result.put("status", false);
            result.put("message", e.getMessage());
        }

        return new ResponseEntity<>(result , HttpStatus.OK);
    }

    @PostMapping("/join")
    @ApiOperation(value = "???????????? ?????? => email, password??? ??????")
    public Object tempJoin(@RequestBody TempUserDto tempUserDto) {
        Map<String, Object> result = new HashMap<>();
        String emailCode = userService.randomCode();
        try {
            userService.tempSaveAndSendEmail(TempUserDto.builder()
                    .email(tempUserDto.getEmail()).password(passwordEncoder.encode(tempUserDto.getPassword())).token(emailCode).isCheck(false).timeToLive(10).build(), tempUserDto.getEmail());
            result.put("status", true);
            result.put("message", "???????????? ?????? ??????");
        } catch (Exception e) {
            result.put("status", false);
            result.put("message", e.getMessage());
        }

        return new ResponseEntity<>(result , HttpStatus.OK);
    }

    @PostMapping("/join/codecheck")
    @ApiOperation(value = "???????????? ?????? => email, token ??????")
    public Object checkEmailCode(@RequestBody TempUserDto tempUser) {
        Map<String, Object> result = new HashMap<>();
        Optional<TempUserDto> tempUserDto;
        try {
            tempUserDto = userService.tempUserByEmail(tempUser.getEmail());
        } catch (Exception e) {
            result.put("status", false);
            result.put("message", "?????? ????????? ?????? ??????");
            return new ResponseEntity<>(result , HttpStatus.OK);
        }
        if(tempUserDto.isPresent()) {
            if(!tempUserDto.get().getToken().equals(tempUser.getToken())) {
                result.put("status", false);
                result.put("message", "???????????? ?????????");
            } else {
                result.put("status", true);
                result.put("message", "???????????? ??????.");
                tempUserDto.get().setCheck(true);
                tempUserDto.get().setTimeToLive(30);
                userService.tempUserSave(tempUserDto.get());
            }
        } else {
            result.put("status", false);
            result.put("message", "???????????? ?????? ??????????????????.");
        }

        return new ResponseEntity<>(result , HttpStatus.OK);
    }

    @GetMapping("/join/resend")
    @ApiOperation(value = "????????? ????????? => email??? ??????")
    public Object tempJoin(@RequestParam(required = true) String email) {
        Map<String, Object> result = new HashMap<>();
        String emailCode = userService.randomCode();
        Optional<TempUserDto> tempUserDto;
        try {
            tempUserDto = userService.tempUserByEmail(email);
        } catch (Exception e) {
            result.put("status", false);
            result.put("message", "?????? ????????? ?????? ??????");
            return new ResponseEntity<>(result , HttpStatus.OK);
        }
        if(!tempUserDto.isPresent()) {
            result.put("status", false);
            result.put("message", "???????????? ?????? ??????????????????.");
        } else {
            tempUserDto.get().setToken(emailCode);

            try {
                userService.tempSaveAndSendEmail(TempUserDto.builder()
                        .email(tempUserDto.get().getEmail()).password(tempUserDto.get().getPassword()).token(emailCode).isCheck(false).timeToLive(10).build(), tempUserDto.get().getEmail());
                result.put("status", true);
                result.put("message", "????????? ????????? ??????.");
            } catch (Exception e) {
                result.put("status", false);
                result.put("message", e.getMessage());
            }
        }
        return new ResponseEntity<>(result , HttpStatus.OK);
    }

    @PostMapping("/join/profile")
    @ApiOperation(value = "???????????? ?????? => email, image(?????? ??????x), nickname, githubid, position ?????? multipart???? ????????????")
    public Object join(@RequestBody UserDto userDto) {
        Map<String, Object> result = new HashMap<>();
        Optional<TempUserDto> tempUserDto;
        try {
            tempUserDto = userService.tempUserByEmail(userDto.getEmail());
        } catch (Exception e) {
            result.put("status", false);
            result.put("message", "?????? ????????? ?????? ??????");
            return new ResponseEntity<>(result , HttpStatus.OK);
        }
        if(!tempUserDto.isPresent()) {
            result.put("status", false);
            result.put("message", "???????????? ?????? ??????????????????.");
        } else {
            if(!tempUserDto.get().isCheck()) {
                result.put("status", false);
                result.put("message", "???????????? ??????.");
                return new ResponseEntity<>(result , HttpStatus.OK);
            }
            try {
                String userid = userService.createUserid();
                userService.userSave(User.builder()
                        .userid(userid)
                        .email(userDto.getEmail())
                        .password(tempUserDto.get().getPassword())
                        .nickname(userDto.getNickname())
                        .githubid(userDto.getGithubid())
                        .positions(userService.positionsName(userDto.getPosition()))
                        .image("https://ohnaco.s3.ap-northeast-2.amazonaws.com/defaultProfile")
                        .build());

                userService.tempUserDelete(userDto.getEmail());
                if(!"".equals(userDto.getGithubid())) {
                    todoService.commitUpdateWeekend(userid, userDto.getGithubid());
                }
                result.put("status", true);
                result.put("message", "???????????? ??????.");
            } catch (Exception e) {
                result.put("status", false);
                result.put("message", "??????????????????.");
            }
        }
        return new ResponseEntity<>(result , HttpStatus.OK);
    }

    @PostMapping("/findpwd")
    @ApiOperation(value = "?????????????????? => email")
    public Object findPwd(@RequestBody Map<String, String> map) {
        Map<String, Object> result = new HashMap<>();
        String emailCode = userService.randomCode();
        if(userService.findByEmail(map.get("email")) == null) {
            result.put("status", false);
            result.put("message", "???????????? ???????????? ????????????.");
            return new ResponseEntity<>(result, HttpStatus.OK);
        }
        try {
            userService.tempSaveAndSendEmail(TempUserDto.builder().email("findPwd:"+map.get("email")).token(emailCode).timeToLive(5).build(), map.get("email"));
            result.put("status", true);
            result.put("message", "???????????? ?????? ??? ?????? ??????");
        } catch (Exception e) {
            result.put("status", false);
            result.put("message", e.getMessage());
        }

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @PutMapping("/findpwd")
    @ApiOperation(value = "????????? ???????????? ?????? => email, newpwd")
    public Object changeNewPwd(@RequestBody FindPasswordDto findPasswordDto) {
        Map<String, Object> result = new HashMap<>();
        try {
            if(userService.isTokenConfirm(findPasswordDto.getEmail(), findPasswordDto.getCode())) {
                User user = userService.findByEmail(findPasswordDto.getEmail());
                userService.userSave(User.builder().userid(user.getUserid()).email(user.getEmail()).password(passwordEncoder.encode(findPasswordDto.getPassword())).nickname(user.getNickname()).positions(user.getPositions()).githubid(user.getGithubid()).image(user.getImage()).build());
                result.put("status", "success");
            } else result.put("status", "fail");
        } catch (Exception e) {
            result.put("status", "error");
            e.printStackTrace();
        }
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}