package com.walter.speech;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = SpeechApplication.class)
public class SpeechEvaluationTest {

    @Autowired
    private SpeechEvaluation speechEvaluation;

    @Test
    public void getEvaluationRecord() {
        System.out.println(speechEvaluation.getEvaluationRecord("hello", "d:" + File.separator + "text2audio.mp3"));
    }
}