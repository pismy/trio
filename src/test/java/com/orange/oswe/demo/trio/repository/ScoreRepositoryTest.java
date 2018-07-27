package com.orange.oswe.demo.trio.repository;

import com.orange.oswe.demo.trio.domain.Result;
import com.orange.oswe.demo.trio.domain.Score;
import com.orange.oswe.demo.trio.domain.User;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;

@RunWith(SpringRunner.class)
//@SpringBootTest({"spring.datasource.url=jdbc:h2:mem:trio;MODE=MySQL;DATABASE_TO_UPPER=TRUE;DB_CLOSE_ON_EXIT=FALSE;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS \"public\";"})
@SpringBootTest()
@DataJpaTest
@ActiveProfiles("h2")
//@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class ScoreRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ResultRepository resultRepository;

    @Autowired
    private ScoreRepository scoreRepository;

    @Test
    public void test1() {
        User user1 = new User("user1", "user1", "password");
        userRepository.save(user1);
        User user2 = new User("user2", "user2", "password");
        userRepository.save(user2);

        Result result1 = Result.build(Result.Id.build("result1", 1), user1, new Date(), null);
        result1.setScores(new HashSet<>(Arrays.asList(
                Score.build(null, result1, user1, 10),
                Score.build(null, result1, user2, 5)
        )));
        resultRepository.save(result1);

        Result result2 = Result.build(Result.Id.build("result1", 2), user1, new Date(), null);
        result2.setScores(new HashSet<>(Arrays.asList(
                Score.build(null, result2, user1, 10),
                Score.build(null, result2, user2, 5)
        )));
        resultRepository.save(result2);

        Result result3 = Result.build(Result.Id.build("result2", 1), user1, new Date(), null);
        result3.setScores(new HashSet<>(Arrays.asList(
                Score.build(null, result3, user1, 10),
                Score.build(null, result3, user2, 5)
        )));
        resultRepository.save(result3);


        System.out.println(resultRepository.findAll());
    }
}