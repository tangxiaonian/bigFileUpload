package com.example.demo.service;

import com.example.demo.domain.MultipartFileParam;

import java.io.InputStream;
import java.io.OutputStream;

public interface FileService {

    boolean fileUpload(MultipartFileParam multipartFileParam);

    InputStream getUploadFile(String id) throws Exception;
}
