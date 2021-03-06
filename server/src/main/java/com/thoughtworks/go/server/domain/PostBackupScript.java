/*
 * Copyright 2018 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thoughtworks.go.server.domain;

import com.google.common.collect.ImmutableMap;
import com.google.gson.internal.bind.util.ISO8601Utils;
import com.thoughtworks.go.server.service.BackupService;
import com.thoughtworks.go.util.command.CommandLine;
import com.thoughtworks.go.util.command.CommandLineException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PostBackupScript {
    private static final Logger LOG = LoggerFactory.getLogger(PostBackupScript.class);

    private final String postBackupScript;
    private final BackupService.BackupInitiator initiatedBy;
    private final Username username;
    private final ServerBackup backup;
    private final String backupBaseDir;

    public PostBackupScript(String postBackupScript,
                            BackupService.BackupInitiator initiatedBy,
                            Username username,
                            ServerBackup backup,
                            String backupBaseDir) {
        this.postBackupScript = postBackupScript;
        this.initiatedBy = initiatedBy;
        this.username = username;
        this.backup = backup;
        this.backupBaseDir = backupBaseDir;
    }

    public boolean execute() {
        try {
            commandLine().runOrBomb("Post Backup Script");
            return true;
        } catch (CommandLineException e) {
            LOG.error("Failed to execute post backup script.\n{}", e.getResult().describe(), e);
            return false;
        }
    }

    CommandLine commandLine() {
        ImmutableMap.Builder<String, String> envBuilder = new ImmutableMap.Builder<>();

        if (backup == null) {
            envBuilder.put("GOCD_BACKUP_STATUS", "failure");
        } else {
            envBuilder.put("GOCD_BACKUP_STATUS", "success")
                    .put("GOCD_BACKUP_BASE_DIR", backupBaseDir)
                    .put("GOCD_BACKUP_PATH", backup.getPath())
                    .put("GOCD_BACKUP_TIMESTAMP", ISO8601Utils.format(backup.getTime()));
        }

        switch (initiatedBy) {
            case TIMER:
                envBuilder.put("GOCD_BACKUP_INITIATED_VIA", "TIMER");
                break;
            case USER:
                envBuilder.put("GOCD_BACKUP_INITIATED_BY_USER", username.getUsername().toString());
                break;
        }

        return CommandLine.createCommandLine(postBackupScript)
                .withEncoding("UTF-8")
                .withEnv(envBuilder.build());

    }
}
