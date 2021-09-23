/*
    Copyright 2018 Adaptris

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/

package com.adaptris.aws.s3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.adaptris.annotation.AdapterComponent;
import com.adaptris.annotation.ComponentProfile;
import com.adaptris.annotation.DisplayOrder;
import com.adaptris.core.AdaptrisMessage;
import com.amazonaws.services.s3.AmazonS3Client;
import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Create a bucket in S3.
 *
 * @config amazon-s3-create-bucket
 */
@AdapterComponent
@ComponentProfile(summary = "Create a bucket in S3")
@XStreamAlias("amazon-s3-create-bucket")
@DisplayOrder(order = {"bucket"})
public class CreateBucketOperation extends S3OperationImpl {

  protected transient Logger log = LoggerFactory.getLogger(this.getClass().getName());

  public CreateBucketOperation() {
  }

  @Override
  public void execute(ClientWrapper wrapper, AdaptrisMessage msg) throws Exception {
    AmazonS3Client s3 = wrapper.amazonClient();
    String bucket = s3Bucket(msg);
    log.trace("Creating Bucket [{}]", bucket);
    s3.createBucket(bucket);
  }
}