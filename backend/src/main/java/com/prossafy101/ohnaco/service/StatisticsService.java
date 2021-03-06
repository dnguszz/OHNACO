package com.prossafy101.ohnaco.service;

import com.prossafy101.ohnaco.entity.statistics.StatisticsCategoryDto;
import com.prossafy101.ohnaco.entity.statistics.StatisticsPositionDto;
import com.prossafy101.ohnaco.entity.user.User;
import com.prossafy101.ohnaco.repository.StatisticsRepository;
import com.prossafy101.ohnaco.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
public class StatisticsService {
    @Autowired
    private StatisticsRepository statisticsRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RedisUtil redisUtil;

    String[] category = {"CS", "알고리즘", "프레임워크", "자격증", "기타"};

    //해당 유저의 주간/월간 시간과 목표시간 총합
    public Long getToatalTime(String userid, String startDate, String endDate) {
        Map<String, Object> map = new HashMap<>();
        map.put("startDate", LocalDateTime.of(LocalDate.parse(startDate), LocalTime.of(0,0,0)));
        map.put("endDate", LocalDateTime.of(LocalDate.parse(endDate), LocalTime.of(23,59,59)));
        map.put("userid", userid);
        return statisticsRepository.getTotalTime(map);
    }


    //해당 유저의 카테고리별 시간과 목표시간 총합
    public List<StatisticsCategoryDto> getCategoryTime(String userid, String startDate, String endDate) {
        Map<String, Object> map = new HashMap<>();
        map.put("startDate", LocalDateTime.of(LocalDate.parse(startDate), LocalTime.of(0,0,0)));
        map.put("endDate", LocalDateTime.of(LocalDate.parse(endDate), LocalTime.of(23,59,59)));
        map.put("userid", userid);
        boolean[] isCategory = new boolean[5];
        long totalcomplete = 0;
        long totalgoal = 0;
        List<StatisticsCategoryDto> list = statisticsRepository.getCategoryTime(map);
        for(StatisticsCategoryDto dto : list) {
            totalcomplete += dto.getCompletetime();
            totalgoal += dto.getGoaltime();
            for(int i = 0 ; i < 5;  i++) {
                if(dto.getCategoryname().equals(category[i])) {
                    isCategory[i] = true;
                }
            }
        }

        for(int i = 0 ; i < 5;  i++) {
            if(!isCategory[i]) {
                list.add(new StatisticsCategoryDto(category[i], 0L, 0L));
            }
        }
        Collections.sort(list, new Comparator<StatisticsCategoryDto>() {
            @Override
            public int compare(StatisticsCategoryDto o1, StatisticsCategoryDto o2) {
                return o1.getCategoryname().compareTo(o2.getCategoryname());
            }
        });
        StatisticsCategoryDto entire = new StatisticsCategoryDto("entire", totalcomplete, totalgoal);
        System.out.println("totalgoal" + totalgoal);
        System.out.println("entire " + entire.getCompletetime() + " " + entire.getGoaltime());
        list.add(entire);
        return list;
    }

    public List<StatisticsPositionDto> getPositionTime(String userid, String startDate, String endDate) {
        Map<String, Object> map = new HashMap<>();
        int positionid = userRepository.findByUserid(userid).getPositions().getPositionid();
        map.put("startDate", LocalDateTime.of(LocalDate.parse(startDate), LocalTime.of(0,0,0)));
        map.put("endDate", LocalDateTime.of(LocalDate.parse(endDate), LocalTime.of(23,59,59)));
        map.put("positionid", positionid);
        boolean[] isCategory = new boolean[5];
        List<StatisticsPositionDto> list = statisticsRepository.getPositionTime(map);
        for(StatisticsPositionDto dto : list) {
            for(int i = 0 ; i < 5;  i++) {
                if(dto.getCategoryname().equals(category[i])) {
                    isCategory[i] = true;
                }
            }
        }

        for(int i = 0 ; i < 5;  i++) {
            if(!isCategory[i]) {
                list.add(new StatisticsPositionDto(category[i], 0L));
            }
        }
        Collections.sort(list, new Comparator<StatisticsPositionDto>() {
            @Override
            public int compare(StatisticsPositionDto o1, StatisticsPositionDto o2) {
                return o1.getCategoryname().compareTo(o2.getCategoryname());
            }
        });
        return list;
    }

    public List<StatisticsPositionDto> getEntireCategoryTime(String startDate, String endDate) {
        Map<String, Object> map = new HashMap<>();
        map.put("startDate", LocalDateTime.of(LocalDate.parse(startDate), LocalTime.of(0,0,0)));
        map.put("endDate", LocalDateTime.of(LocalDate.parse(endDate), LocalTime.of(23,59,59)));
        boolean[] isCategory = new boolean[5];
        List<StatisticsPositionDto> list = statisticsRepository.getEntireCategoryTime(map);
        for(StatisticsPositionDto dto : list) {
            for(int i = 0 ; i < 5;  i++) {
                if(dto.getCategoryname().equals(category[i])) {
                    isCategory[i] = true;
                }
            }
        }

        for(int i = 0 ; i < 5;  i++) {
            if(!isCategory[i]) {
                list.add(new StatisticsPositionDto(category[i], 0L));
            }
        }
        Collections.sort(list, new Comparator<StatisticsPositionDto>() {
            @Override
            public int compare(StatisticsPositionDto o1, StatisticsPositionDto o2) {
                return o1.getCategoryname().compareTo(o2.getCategoryname());
            }
        });
        return list;
    }

    public List<Map<String, Object>> getTotalTimeForDays(String startDate, String endDate) {
        Map<String, Object> map = new HashMap<>();
        map.put("startDate", LocalDateTime.of(LocalDate.parse(startDate), LocalTime.of(0,0,0)));
        map.put("endDate", LocalDateTime.of(LocalDate.parse(endDate), LocalTime.of(23,59,59)));
        List<Map<String, Object>> list = statisticsRepository.getTotalTimeForDays(map);
        Calendar cal = Calendar.getInstance();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        String[] split = startDate.split("-");
        cal.set(Integer.parseInt(split[0]), Integer.parseInt(split[1])-1, Integer.parseInt(split[2]));
        for(int i=0; i<list.size(); i++) {
            if(!list.get(i).containsKey("time")) {
                list.get(i).put("time", 0L);
            }
            if(!list.get(i).get("date").toString().equals(df.format(cal.getTime()))) {
                map = new HashMap<>();
                map.put("date", df.format(cal.getTime()));
                map.put("time", 0L);
                list.add(i, map);
            }
            cal.add(Calendar.DATE, 1);
        }
        for(int i=list.size(); i<7; i++) {
            map = new HashMap<>();
            map.put("date", df.format(cal.getTime()));
            map.put("time", 0L);
            list.add(map);
            cal.add(Calendar.DATE, 1);
        }

        return list;
    }

    public List<Map<String, Object>> getPositionTimeForDays(String userid, String startDate, String endDate) {
        Map<String, Object> map = new HashMap<>();
        int positionid = userRepository.findByUserid(userid).getPositions().getPositionid();
        map.put("startDate", LocalDateTime.of(LocalDate.parse(startDate), LocalTime.of(0,0,0)));
        map.put("endDate", LocalDateTime.of(LocalDate.parse(endDate), LocalTime.of(23,59,59)));
        map.put("positionid", positionid);
        List<Map<String, Object>> list = statisticsRepository.getPositionTimeForDays(map);
        Calendar cal = Calendar.getInstance();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        String[] split = startDate.split("-");
        cal.set(Integer.parseInt(split[0]), Integer.parseInt(split[1])-1, Integer.parseInt(split[2]));
        for(int i=0; i<list.size(); i++) {
            if(!list.get(i).containsKey("time")) {
                list.get(i).put("time", 0L);
            }
            if(!list.get(i).get("date").toString().equals(df.format(cal.getTime()))) {
                map = new HashMap<>();
                map.put("date", df.format(cal.getTime()));
                map.put("time", 0L);
                list.add(i, map);
            }
            cal.add(Calendar.DATE, 1);
        }
        for(int i=list.size(); i<7; i++) {
            map = new HashMap<>();
            map.put("date", df.format(cal.getTime()));
            map.put("time", 0L);
            list.add(map);
            cal.add(Calendar.DATE, 1);
        }

        return list;
    }

    public List<Map<String, Object>> getMyTimeForDays(String userid, String startDate, String endDate) {
        Map<String, Object> map = new HashMap<>();
        map.put("startDate", LocalDateTime.of(LocalDate.parse(startDate), LocalTime.of(0,0,0)));
        map.put("endDate", LocalDateTime.of(LocalDate.parse(endDate), LocalTime.of(23,59,59)));
        map.put("userid", userid);
        List<Map<String, Object>> list = statisticsRepository.getMyTimeForDays(map);
        Calendar cal = Calendar.getInstance();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        String[] split = startDate.split("-");
        cal.set(Integer.parseInt(split[0]), Integer.parseInt(split[1])-1, Integer.parseInt(split[2]));
        for(int i=0; i<list.size(); i++) {
            if(!list.get(i).containsKey("time")) {
                list.get(i).put("time", 0L);
            }
            if(!list.get(i).get("date").toString().equals(df.format(cal.getTime()))) {
                map = new HashMap<>();
                map.put("date", df.format(cal.getTime()));
                map.put("time", 0L);
                list.add(i, map);
            }
            cal.add(Calendar.DATE, 1);
        }
        for(int i=list.size(); i<7; i++) {
            map = new HashMap<>();
            map.put("date", df.format(cal.getTime()));
            map.put("time", 0L);
            list.add(map);
            cal.add(Calendar.DATE, 1);
        }

        return list;
    }

    public List<Map<String, Object>> getMyTimeForWeeks(String userid, String startDate, String endDate) {
        Map<String, Object> map = new HashMap<>();
        map.put("startDate", LocalDateTime.of(LocalDate.parse(startDate), LocalTime.of(0,0,0)));
        map.put("endDate", LocalDateTime.of(LocalDate.parse(endDate), LocalTime.of(23,59,59)));
        map.put("userid", userid);
        List<Map<String, Object>> list = statisticsRepository.getMyTimeForWeeks(map);
        for(int i=0; i<list.size(); i++) {
            if(!list.get(i).containsKey("time")) {
                list.get(i).put("time", 0L);
            }
            if(!list.get(i).get("day").toString().equals(Integer.toString(i+1))) {
                map = new HashMap<>();
                map.put("day", i+1);
                map.put("time", 0L);
                list.add(i, map);
            }
        }
        for(int i=list.size(); i<7; i++) {
            map = new HashMap<>();
            map.put("day", i+1);
            map.put("time", 0L);
            list.add(map);
        }
        return list;
    }

    public List<Map<String, Object>> getPositionTimeForWeeks(String userid, String startDate, String endDate) {
        Map<String, Object> map = new HashMap<>();
        int positionid = userRepository.findByUserid(userid).getPositions().getPositionid();
        map.put("startDate", LocalDateTime.of(LocalDate.parse(startDate), LocalTime.of(0,0,0)));
        map.put("endDate", LocalDateTime.of(LocalDate.parse(endDate), LocalTime.of(23,59,59)));
        map.put("positionid", positionid);
        List<Map<String, Object>> list = statisticsRepository.getPositionTimeForWeeks(map);
        for(int i=0; i<list.size(); i++) {
            if(!list.get(i).containsKey("time")) {
                list.get(i).put("time", 0L);
            }
            if(!list.get(i).get("day").toString().equals(Integer.toString(i+1))) {
                map = new HashMap<>();
                map.put("day", i+1);
                map.put("time", 0L);
                list.add(i, map);
            }
        }
        for(int i=list.size(); i<7; i++) {
            map = new HashMap<>();
            map.put("day", i+1);
            map.put("time", 0L);
            list.add(map);
        }
        return list;
    }

    public List<Map<String, Object>> getTotalTimeForWeeks(String startDate, String endDate) {
        Map<String, Object> map = new HashMap<>();
        map.put("startDate", LocalDateTime.of(LocalDate.parse(startDate), LocalTime.of(0,0,0)));
        map.put("endDate", LocalDateTime.of(LocalDate.parse(endDate), LocalTime.of(23,59,59)));

        List<Map<String, Object>> list = statisticsRepository.getTotalTimeForWeeks(map);
        for(int i=0; i<list.size(); i++) {
            if(!list.get(i).containsKey("time")) {
                list.get(i).put("time", 0L);
            }
            if(!list.get(i).get("day").toString().equals(Integer.toString(i+1))) {
                map = new HashMap<>();
                map.put("day", i+1);
                map.put("time", 0L);
                list.add(i, map);
            }
        }
        for(int i=list.size(); i<7; i++) {
            map = new HashMap<>();
            map.put("day", i+1);
            map.put("time", 0L);
            list.add(map);
        }
        return list;
    }

    public void updateWeekAndMonth(String userid) {
        Map<String, Object> week = new HashMap<>();
        Map<String, Object> month = new HashMap<>();
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");

        cal.add(Calendar.DATE, -1);
        List<StatisticsCategoryDto> todayTime = getCategoryTime(userid, df.format(cal.getTime()), df.format(cal.getTime()));
        week.put("todayTime", todayTime);
        month.put("todayTime", todayTime);

        cal.add(Calendar.DATE, -1);
        List<StatisticsCategoryDto> yesterdayTime = getCategoryTime(userid, df.format(cal.getTime()), df.format(cal.getTime()));
        week.put("yesterdayTime", yesterdayTime);
        month.put("yesterdayTime", yesterdayTime);

        cal.setTime(new Date());
        cal.add(Calendar.DATE, 0);
        String endDate = df.format(cal.getTime());
        cal.add(Calendar.DATE, -6);
        String startDate = df.format(cal.getTime());

        week.put("totalTime", getToatalTime(userid, startDate, endDate));
        week.put("categoryTime", getCategoryTime(userid, startDate, endDate));
        week.put("positionTime", getPositionTime(userid, startDate, endDate));
        week.put("entireCategoryTime", getEntireCategoryTime(startDate, endDate));
        week.put("entireMemberTime", getTotalTimeForDays(startDate, endDate));
        week.put("positionMemberTime", getPositionTimeForDays(userid, startDate, endDate));
        week.put("myTime", getMyTimeForDays(userid, startDate, endDate));

        cal.setTime(new Date());
        cal.add(Calendar.DATE, 0);
        endDate = df.format(cal.getTime());
        cal.add(Calendar.DATE, -30);
        startDate = df.format(cal.getTime());

        month.put("totalTime", getToatalTime(userid, startDate, endDate));
        month.put("categoryTime", getCategoryTime(userid, startDate, endDate));
        month.put("positionTime", getPositionTime(userid, startDate, endDate));
        month.put("entireCategoryTime", getEntireCategoryTime(startDate, endDate));
        month.put("myTime", getMyTimeForWeeks(userid, startDate, endDate));
        month.put("positionMemberTime", getPositionTimeForWeeks(userid, startDate, endDate));
        month.put("entireMemberTime", getTotalTimeForWeeks(startDate, endDate));
        redisUtil.setObject("statistics:week:"+userid, week, 2);
        redisUtil.setObject("statistics:month:"+userid, month, 2);
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void updateStatistics() {
        List<User> users = userRepository.findAll();
        for(User user: users) {
            updateWeekAndMonth(user.getUserid());
        }
    }

}
