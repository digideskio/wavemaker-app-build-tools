/**
 * Copyright (C) 2016 WaveMaker, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wavemaker.studio.app.build.maven.plugin.handler;

import java.util.List;

import com.wavemaker.studio.app.build.page.min.PageMinFileGenerator;
import com.wavemaker.studio.common.io.Folder;

/**
 * Created by saddhamp on 21/4/16.
 */
public class PageMinFileGenerationHandler implements AppBuildHandler {
    private Folder pagesFolder;

    public PageMinFileGenerationHandler(Folder pagesFolder){
        this.pagesFolder = pagesFolder;
    }

    @Override
    public void handle() {

        if(pagesFolder.exists()) {
            List<Folder> pageFolders = pagesFolder.list().folders().fetchAll();
            if (pageFolders.size() > 0){
                PageMinFileGenerator pageMinFileGenerator = new PageMinFileGenerator(pageFolders);
                pageMinFileGenerator.setForceOverwrite(true).generate();
            }
        }
    }
}
