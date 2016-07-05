package com.adaptris.aws.s3;

import static org.apache.commons.lang.StringUtils.isEmpty;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.adaptris.annotation.AdapterComponent;
import com.adaptris.annotation.AdvancedConfig;
import com.adaptris.annotation.ComponentProfile;
import com.adaptris.annotation.DisplayOrder;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.lms.FileBackedMessage;
import com.adaptris.core.util.ExceptionHelper;
import com.adaptris.core.util.ManagedThreadFactory;
import com.adaptris.interlok.InterlokException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.transfer.Download;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.util.IOUtils;
import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Download an object from S3 using {@link TransferManager}.
 * 
 * @author lchan
 * @config amazon-s3-download
 */
@AdapterComponent
@ComponentProfile(summary = "Amazon S3 Download using Transfer Manager")
@XStreamAlias("amazon-s3-download")
@DisplayOrder(order = {"key", "bucketName", "tempDirectory"})
public class DownloadOperation extends S3OperationImpl {

  @AdvancedConfig
  private String tempDirectory;

  private transient ManagedThreadFactory threadFactory = new ManagedThreadFactory();

  public DownloadOperation() {

  }

  @Override
  public void execute(AmazonS3Client s3, AdaptrisMessage msg) throws InterlokException {
    TransferManager tm = new TransferManager(s3);
    File tempDir = null;
    try {
      if (!isEmpty(getTempDirectory())) {
        tempDir = new File(getTempDirectory());
      }
      GetObjectRequest request = new GetObjectRequest(getBucketName().extract(msg), getKey().extract(msg));
      log.debug("Getting {} from bucket {}", request.getKey(), request.getBucketName());
      File destFile = File.createTempFile(this.getClass().getSimpleName(), "", tempDir);
      Download download = tm.download(request, destFile);
      Thread t = threadFactory.newThread(new MyProgressListener(download));
      t.setName(Thread.currentThread().getName());
      t.start();
      download.waitForCompletion();
      write(destFile, msg);
    } catch (Exception e) {
      throw ExceptionHelper.wrapServiceException(e);
    }
  }


  private void write(File f, AdaptrisMessage msg) throws IOException {
    if (msg instanceof FileBackedMessage) {
      log.trace("Initialising Message from {}", f.getCanonicalPath());
      ((FileBackedMessage) msg).initialiseFrom(f);
    } else {
      try (FileInputStream in = new FileInputStream(f); OutputStream out = msg.getOutputStream()) {
        IOUtils.copy(in, out);
      }
    }
  }

  /**
   * @return the tempDirectory
   */
  public String getTempDirectory() {
    return tempDirectory;
  }


  /**
   * Set the temp directory to store files.
   * 
   * @param s the tempDirectory to set, if not specified defaults to {@code java.io.tmpdir}
   */
  public void setTempDirectory(String s) {
    this.tempDirectory = s;
  }

  private class MyProgressListener implements Runnable {
    private Download download;

    MyProgressListener(Download download) {
      this.download = download;
    }

    public void run() {
      while (!download.isDone()) {
        log.trace("Downloaded : {}%", (download.getProgress().getPercentTransferred() / 1));
        try {
          Thread.sleep(2000);
        } catch (InterruptedException e) {
        }
      }
    }
  }

}