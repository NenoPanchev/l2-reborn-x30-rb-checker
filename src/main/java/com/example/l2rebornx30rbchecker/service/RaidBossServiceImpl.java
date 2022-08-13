package com.example.l2rebornx30rbchecker.service;

import com.example.l2rebornx30rbchecker.model.entity.RaidBoss;
import com.example.l2rebornx30rbchecker.model.view.RaidBossViewModel;
import com.example.l2rebornx30rbchecker.repository.RaidBossRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.modelmapper.ModelMapper;
import org.openqa.selenium.WebDriver;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.example.l2rebornx30rbchecker.constants.GlobalConstants.*;

@Service
public class RaidBossServiceImpl implements RaidBossService {
    private final RaidBossRepository raidBossRepository;
    private final DriverServiceImpl driverService;
    private final ModelMapper modelMapper;
//    private final AudioServiceImpl audioService;
    private final WebDriver driver;

    public RaidBossServiceImpl(RaidBossRepository raidBossRepository, DriverServiceImpl driverService, ModelMapper modelMapper, WebDriver driver) {
        this.raidBossRepository = raidBossRepository;
        this.driverService = driverService;
        this.modelMapper = modelMapper;
        this.driver = driver;
    }

    @Override
    public void seedRaidBoss(RaidBoss rb) {
        if (SUB_AND_ALLY_BOSSES.contains(rb.getName())) {
            rb.setRespawnTime(8L);
        } else if (rb.getName().equals(BARAKIEL)) {
            rb.setRespawnTime(6L);
        }
        String rebornId = namesIDs.get(rb.getName());
        rb.setRebornID(rebornId)
                .setDropURL(String.format("https://interlude.wiki/db/npc/%s.html", rebornId))
                .setLocationURL(String.format("https://interlude.wiki/db/loc/%s.html", rebornId));
        raidBossRepository.save(rb);
    }

    @Override
    public List<RaidBossViewModel> getAllRaidBosses() {
        return raidBossRepository
                .findAllByOrderByRespawnEnd()
                .stream()
                .map(entity -> modelMapper.map(entity, RaidBossViewModel.class)
                .setRespawnStart(entity.getRespawnStart() == null ? "" : getTimeFrom(entity.getRespawnStart()))
                .setRespawnEnd(entity.getRespawnEnd() == null ? "" : Arrays.stream(getTimeFrom(entity.getRespawnEnd()).split(" - ")).skip(1).findFirst().orElse(""))
                        .setRespawnStartTime(entity.getRespawnStart() == null ? null : entity.getRespawnStart())
                .setTimeOfDeath(entity.getTimeOfDeath() == null ? "" : getTimeFrom(entity.getTimeOfDeath())))
                .collect(Collectors.toList());
    }

    private String getTimeFrom(LocalDateTime localDateTime) {
        String[] str = localDateTime.toString().split("T");
        String hour = str[1].substring(0, 5);
        String[] date = str[0].split("-");
        String day = date[2];
        String month = date[1];
        return String.format("%s.%s  -  %s", day, month, hour);
    }

    @Override
    public void updateInfo() {
        driver.get(SITE_URL);
        Document doc = Jsoup.parse(driver.getPageSource());
        List<RaidBoss> raidBosses = driverService.parseHTMLIntoRBInfo(doc);
        boolean alive = false;

        for (RaidBoss rb : raidBosses) {
            if (!raidBossRepository.existsByName(rb.getName())) {
                seedRaidBoss(rb);
                continue;
            }

            RaidBoss rbEntity = raidBossRepository.findByName(rb.getName()).orElseThrow();

            if (rb.isAlive()) {
                rbEntity.setRespawnStart(null)
                        .setRespawnEnd(null)
                        .setTimeOfDeath(null)
                        .setAlive(true);
                raidBossRepository.save(rbEntity);
                continue;
            }

            if (rbEntity.isAlive() && !rb.isAlive()) {
                rbEntity.setRespawnStart(rb.getRespawnStart())
                        .setAlive(rb.isAlive())
                        .setRespawnEnd(rb.getRespawnEnd());
                raidBossRepository.save(rbEntity);

//                setRespawnByTimeOfDeath(rbEntity, rb, driverService.getTimeOfUpdate());
                continue;
            }

            Long hoursDifference = Math.abs(Duration.between(rb.getRespawnStart(), rbEntity.getRespawnStart()).toHours());
            if (hoursDifference != 0) {
                    rbEntity.setRespawnStart(rb.getRespawnStart());
                    rbEntity.setRespawnEnd(rb.getRespawnEnd());
                    rbEntity.setTimeOfDeath(null);
                    rbEntity.setAlive(false);
                    raidBossRepository.save(rbEntity);
            }
        }
//        if (alive) {
//            audioService.playSound();
//        }
    }

    @Override
    public void updateTimeOfDeath(String name, LocalDateTime timeOfDeath) {
        RaidBoss rbEntity = raidBossRepository.findByName(name).orElseThrow();
        rbEntity.setTimeOfDeath(timeOfDeath);
        Long hoursDifference = Duration.between(timeOfDeath, rbEntity.getRespawnStart()).toHours();

        int minuteOfDeath = timeOfDeath.getMinute();
        if (hoursDifference < rbEntity.getRespawnTime()) {
            rbEntity.setRespawnStart(rbEntity.getRespawnStart().plusMinutes(minuteOfDeath));
        } else {
            rbEntity.setRespawnEnd(rbEntity.getRespawnStart().plusMinutes(minuteOfDeath));
        }
        raidBossRepository.save(rbEntity);
    }

    private void setRespawnByTimeOfDeath(RaidBoss rbEntity, RaidBoss rbNewInfo, LocalDateTime timeOfDeath) {
        rbEntity.setTimeOfDeath(timeOfDeath);
        rbEntity.setAlive(false);

        Long hoursDifference = Duration.between(timeOfDeath, rbNewInfo.getRespawnStart()).toHours();
        int minuteOfDeath = timeOfDeath.getMinute();
        if (hoursDifference < rbEntity.getRespawnTime()) {
            rbEntity.setRespawnStart(rbNewInfo.getRespawnStart().plusMinutes(minuteOfDeath));
            rbEntity.setRespawnEnd(rbNewInfo.getRespawnEnd());
        } else {
            rbEntity.setRespawnStart(rbNewInfo.getRespawnStart());
            rbEntity.setRespawnEnd(rbNewInfo.getRespawnStart().plusMinutes(minuteOfDeath));
        }
        raidBossRepository.save(rbEntity);
    }
}
