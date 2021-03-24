package com.example.uploadingfiles.storage;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface StorageService {

	void process(MultipartFile fw, MultipartFile dep, String ip1, String ip2);

	List<String> getResult();

}
