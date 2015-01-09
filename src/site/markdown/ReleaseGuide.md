<!--
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-->

<head>
  <title>Release Guide</title>
</head>

## Releasing Apache Twill

The guide describes the steps to build and release Apache Twill artifacts.

### Environment Setup

1. Generate a GPG keypair for signing artifacts if you don't one.
   See [GPG key generation](http://www.apache.org/dev/openpgp.html#generate-key) on how to do so.
   Make sure the public key is published to public key server, as well as added to the
   [KEYS](https://dist.apache.org/repos/dist/release/incubator/twill/KEYS) file.
1. Add the following section to your Maven settings (`~/.m2/settings.xml`):

   ```
   <server>
     <id>apache.releases.https</id>
     <username>[APACHE_USER_ID]</username>
     <password>[ENCRYPTED_APACHE_PASSWORD]</password>
   </server>
   ```
   To generate encrypted password in maven, please consult
   [Maven Encryption Guide](http://maven.apache.org/guides/mini/guide-encryption.html).

### Prepare Release Artifacts

mvn release:prepare

1. Create a new release branch:

   ```
   git branch -a branch-[RELEASE_VERSION]
   ```
   The `[RELEASE_VERSION]` is something like `0.5.0`.

1. Create a new signed tag for the release:

   ```
   git tag -s v[RELEASE_VERSION]-incubating -m 'Releasing [RELEASE_VERSION]-incubating'
   ```
1. Push both the new branch and the tag:

   ```
   git push origin release/[RELEASE_VERSION]
   git push origin v[RELEASE_VERSION]
   ```
1. Run `gpg-agent` to save the number of times that you have to type in your GPG password
   when building release artifacts. You can run `gpg -ab` to confirm the agent is running and
   cached your password correctly. Alternatively, if you don't want to run GPG agent, you can
   specify your GPG password through `-Dgpg.passphrase=[GPG_PASSWORD]` when running Maven.
1. Build the source tarball and publish stage artifacts to the staging repo:

   ```
   mvn clean package -DskipTests -P hadoop-2.0 && 
   mvn package -DskipTests -P hadoop-2.3 && 
   mvn deploy -DskipTests -P hadoopp-2.3 -P apache-release
   ```
   The source tarball could be found in `target/twill-[RELEASE_VERSION]-incubating-source-release.tar.gz`
   after the above command completed successfully.
1. Compute the MD5 and SHA512 of the source release tarball.   

   ```
   cd target
   md5 -q twill-[RELEASE_VERSION]-incubating-source-release.tar.gz > twill-[RELEASE_VERSION]-incubating-source-release.tar.gz.md5
   shasum -a 512 twill-[RELEASE_VERSION]-incubating-source-release.tar.gz > twill-[RELEASE_VERSION]-incubating-source-release.tar.gz.sha512
   ```
1. Checkin the source release tarball, together with the signature, md5 and sha512 files to `dist.apache.org/repos/dist/dev/incubator/twill/[RELEASE_VERSION]-incubating-rc1/src/`
1. Create a `CHANGES.txt` file to describe the changes in the release and checkin the file to `dist.apache.org/repos/dist/dev/incubator/twill/[RELEASE_VERSION]-incubating-rc1/CHANGES.txt`
1. Go to [https://repository.apache.org](https://repository.apache.org) and close the staging repository.
1. Create a vote in the dev mailing list and wait for 72 hours for the vote result. Here is a tempalte of the email:

   ```
   Subject: [VOTE] Release Apache Twill-[RELEASE_VERSION]-incubating [rc1]
   =======================================================================
   
   Hi all,

   This is to call for a vote on releasing Apache Twill [RELEASE_VERSION]-incubating, release candidate 1. This
   is the [Nth] release for Twill.
   
   The source tarball, including signatures, digests, etc can be found at:
   https://dist.apache.org/repos/dist/dev/incubator/twill/[RELEASE_VERSION]-incubating-rc1/src
   
   The tag to be voted upon is v[RELEASE_VERSION]-incubating:
   https://git-wip-us.apache.org/repos/asf?p=incubator-twill.git;a=shortlog;h=refs/tags/v[RELEASE_VERSION]-incubating
   
   The release hash is [ref]
   https://git-wip-us.apache.org/repos/asf?p=incubator-twill.git;a=commit;h=[ref]
   
   The Nexus Staging URL:
   https://repository.apache.org/content/repositories/orgapachetwill-[stageid]
   
   Release artifacts are signed with the following key:
   [URL_TO_SIGNER_PUBLIC_KEY]
   
   KEYS file available here:
   https://dist.apache.org/repos/dist/dev/incubator/twill/KEYS
   
   For information about the contents of this release see:
   https://dist.apache.org/repos/dist/dev/incubator/twill/[RELEASE_VERSION]-incubating-rc1/CHANGES.txt
   
   Please vote on releasing this package as Apache Twill [RELEASE_VERSION]-incubating
   
   The vote will be open for 72 hours.
   
   [ ] +1 Release this package as Apache Twill [RELEASE_VERSION]-incubating
   [ ] +0 no opinion
   [ ] -1 Do not release this package because ...
   
   Thanks,
   Terence
   ```
