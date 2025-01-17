/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.move.ide.newProject

import com.intellij.ide.util.projectWizard.AbstractNewProjectStep
import com.intellij.ide.util.projectWizard.ProjectSettingsStepBase
import com.intellij.platform.DirectoryProjectGenerator

class MvProjectSettingsStep(generator: DirectoryProjectGenerator<NewProjectData>) :
    ProjectSettingsStepBase<NewProjectData>(
        generator,
        AbstractNewProjectStep.AbstractCallback()
    )
