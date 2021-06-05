package com.az.assignment.service.impl;

import com.az.assignment.util.FileUtil;
import com.az.assignment.exception.CustomRuntimeException;
import com.az.assignment.service.UserService;
import com.az.assignment.validator.FileValidator;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final JobLauncher jobLauncher;
    private final Job job;
    private final FileValidator fileValidator;

    @Override
    public void processCSVFile(MultipartFile file) {
        fileValidator.validateFile(file);
        createFileForSpringBatchJob(file);
        launchSpringBatchJob();
    }

    private void createFileForSpringBatchJob(MultipartFile file) {
        log.debug("Create new file for Spring Batch job");
        OutputStream outputStream = null;
        try(InputStream initialStream = file.getInputStream()) {
            byte[] buffer = new byte[initialStream.available()];
            initialStream.read(buffer);
            File targetFile = new File("users.csv");
            outputStream = new FileOutputStream(targetFile);
            outputStream.write(buffer);
            FileUtil.closeOutputStream(outputStream);
        } catch (Exception e) {
            log.error("Exception occurs: ", e);
            throw new CustomRuntimeException("Exception occurs while processing file", HttpStatus.FAILED_DEPENDENCY);
        } finally {
            FileUtil.closeOutputStream(outputStream);
        }
    }

    private void launchSpringBatchJob() {
        Map<String, JobParameter> jobParameterMap = new HashMap<>();
        JobParameters parameters = new JobParameters(jobParameterMap);
        try {
            log.info("Spring Batch Job Start");
            jobLauncher.run(job, parameters);
        } catch (JobExecutionAlreadyRunningException | JobRestartException |
                JobInstanceAlreadyCompleteException | JobParametersInvalidException e) {
            throw new CustomRuntimeException("Exception occurs while processing file", HttpStatus.EXPECTATION_FAILED);
        }
    }
}
