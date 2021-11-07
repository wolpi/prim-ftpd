-- Licensed to the Apache Software Foundation (ASF) under one
-- or more contributor license agreements.  See the NOTICE file
-- distributed with this work for additional information
-- regarding copyright ownership.  The ASF licenses this file
-- to you under the Apache License, Version 2.0 (the
-- "License"); you may not use this file except in compliance
-- with the License.  You may obtain a copy of the License at
-- 
--  http://www.apache.org/licenses/LICENSE-2.0
-- 
-- Unless required by applicable law or agreed to in writing,
-- software distributed under the License is distributed on an
-- "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
-- KIND, either express or implied.  See the License for the
-- specific language governing permissions and limitations
-- under the License.

CREATE TABLE FTP_USER (      
   userid VARCHAR(64) NOT NULL PRIMARY KEY,       
   userpassword VARCHAR(64),      
   homedirectory VARCHAR(128) NOT NULL,             
   enableflag BOOLEAN DEFAULT TRUE,    
   writepermission BOOLEAN DEFAULT FALSE,       
   idletime INT DEFAULT 0,             
   uploadrate INT DEFAULT 0,             
   downloadrate INT DEFAULT 0,
   maxloginnumber INT DEFAULT 0,
   maxloginperip INT DEFAULT 0
);

-- password="pw1"
INSERT INTO FTP_USER (userid, userpassword, homedirectory) VALUES ('user1', '6E6FDF956D04289354DCF1619E28FE77', 'home');

-- password="pw2"
INSERT INTO FTP_USER VALUES ('user2', '6D5779B9B85BD4F11E44C9772E0DE602', 'home', false, true, 2, 5, 1, 3, 4);

-- password=""
INSERT INTO FTP_USER (userid, userpassword, homedirectory) VALUES ('user3', 'D41D8CD98F00B204E9800998ECF8427E', 'home');

-- password="admin"
INSERT INTO FTP_USER (userid, userpassword, homedirectory) VALUES ('admin', '21232F297A57A5A743894A0E4A801FC3', 'home');

