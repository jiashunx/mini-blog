package io.github.jiashunx.app.miniblog.controller;

import com.jfinal.kit.Kv;
import io.github.jiashunx.app.miniblog.exception.MiniBlogException;
import io.github.jiashunx.app.miniblog.model.FileVo;
import io.github.jiashunx.app.miniblog.service.FileService;
import io.github.jiashunx.app.miniblog.util.BlogUtils;
import io.github.jiashunx.masker.rest.framework.MRestFileUploadRequest;
import io.github.jiashunx.masker.rest.framework.MRestRequest;
import io.github.jiashunx.masker.rest.framework.MRestResponse;
import io.github.jiashunx.masker.rest.framework.cons.Constants;
import io.github.jiashunx.masker.rest.framework.filter.MRestFilter;
import io.github.jiashunx.masker.rest.framework.filter.MRestFilterChain;
import io.github.jiashunx.masker.rest.framework.model.MRestFileUpload;
import io.github.jiashunx.masker.rest.framework.util.FileUtils;
import io.github.jiashunx.masker.rest.framework.util.IOUtils;
import io.github.jiashunx.masker.rest.framework.util.MRestHeaderBuilder;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

/**
 * @author jiashunx
 */
public class FileManageController implements MRestFilter {

    private static final Logger logger = LoggerFactory.getLogger(FileManageController.class);

    public static final String FILE_OVERVIEW_URL_PREFIX = "/console/file-manage/file-overview/";
    public static final String FILE_DOWNLOAD_URL_PREFIX = "/console/file-manage/file-download/";

    private final FileService fileService;

    public FileManageController(FileService fileService) {
        this.fileService = Objects.requireNonNull(fileService);
    }

    @Override
    public void doFilter(MRestRequest restRequest, MRestResponse restResponse, MRestFilterChain filterChain) {
        String requestURL = restRequest.getUrl();
        boolean isFileOverview = requestURL.startsWith(FILE_OVERVIEW_URL_PREFIX);
        boolean isFileDownload = requestURL.startsWith(FILE_DOWNLOAD_URL_PREFIX);
        if (isFileOverview || isFileDownload) {
            String fileId = null;
            if (isFileOverview) {
                fileId = requestURL.substring(FILE_OVERVIEW_URL_PREFIX.length());
            } else {
                fileId = requestURL.substring(FILE_DOWNLOAD_URL_PREFIX.length());
            }
            FileVo fileVo = fileService.findOne(fileId);
            if (fileVo == null) {
                restResponse.writeStatusPageAsHtml(HttpResponseStatus.NOT_FOUND);
                return;
            }
            if (isFileOverview) {
                restResponse.write(HttpResponseStatus.OK, fileVo.getFileBytes());
            }
            if (isFileDownload) {
                String tempFilePath = BlogUtils.getTempPath()
                        + "/" + System.currentTimeMillis() + "_" + fileVo.getFileName();
                FileUtils.newFile(tempFilePath);
                File tempFile = new File(tempFilePath);
                try {
                    IOUtils.copy(new ByteArrayInputStream(fileVo.getFileBytes()), tempFile);
                } catch (IOException exception) {
                    throw new MiniBlogException(String.format("download file[id=%s, name=%s] failed."
                            , fileVo.getFileId(), fileVo.getFileName()), exception);
                }
                restResponse.write(tempFile, f -> {
                    FileUtils.deleteFile(tempFile);
                });
            }
        } else {
            filterChain.doFilter(restRequest, restResponse);
        }
    }

    private static final String FILE_MANAGE_HTML = IOUtils.loadContentFromClasspath("template/console/file-manage.html");

    public void fileManageHtml(MRestRequest request, MRestResponse response) {
        List<FileVo> fileVoList = fileService.listAll();
        List<Map<String, Object>> mapList = new ArrayList<>(fileVoList.size());
        for (FileVo fileVo: fileVoList) {
            Map<String, Object> objectMap = new HashMap<>();
            objectMap.put("fileId", fileVo.getFileId());
            objectMap.put("fileName", fileVo.getFileName());
            objectMap.put("createTime", fileVo.getCreateTime() == null ? "undefined" : BlogUtils.format(fileVo.getCreateTime(), BlogUtils.yyyyMMddHHmmssSSS));
            objectMap.put("fileByteSize", fileVo.getFileByteSize());
            mapList.add(objectMap);
        }
        Kv kv = new Kv();
        kv.put("fileVoList", mapList);
        response.write(BlogUtils.render(FILE_MANAGE_HTML, kv)
                , MRestHeaderBuilder.Build(Constants.HTTP_HEADER_CONTENT_TYPE, Constants.CONTENT_TYPE_TEXT_HTML));
    }

    public void delete(MRestRequest request) {
        fileService.deleteOne(request.getParameter("fileId"));
    }

    public void save(MRestRequest request, MRestResponse response) {
        List<FileVo> fileVoList = new LinkedList<>();
        try {
            MRestFileUploadRequest fileUploadRequest = (MRestFileUploadRequest) request;
            List<MRestFileUpload> fileUploadList = fileUploadRequest.getFileUploadList();
            for (MRestFileUpload fileUpload: fileUploadList) {
                InputStream inputStream = fileUpload.getFileInputStream();
                FileVo fileVo = new FileVo();
                fileVo.setFileId(UUID.randomUUID().toString());
                fileVo.setFileName(fileUpload.getFilename());
                ByteArrayOutputStream baos = new ByteArrayOutputStream(inputStream.available());
                IOUtils.copy(inputStream, baos);
                fileVo.setFileBytes(baos.toByteArray());
                fileVo.setFileByteSize(fileVo.getFileBytes().length);
                fileVo.setCreateTime(new Date());
                fileVoList.add(fileVo);
            }
        } catch (Throwable throwable) {
            logger.error("upload file failed.", throwable);
            response.writeStatusPageAsHtml(HttpResponseStatus.INTERNAL_SERVER_ERROR);
        }
        fileService.insert(fileVoList);
    }

}
