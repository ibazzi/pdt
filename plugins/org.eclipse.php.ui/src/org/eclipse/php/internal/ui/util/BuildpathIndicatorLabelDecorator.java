/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.php.internal.ui.util;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.dltk.core.DLTKCore;
import org.eclipse.dltk.core.IModelElement;
import org.eclipse.dltk.core.IScriptProject;
import org.eclipse.dltk.internal.core.ArchiveProjectFragment;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;

public class BuildpathIndicatorLabelDecorator implements ILightweightLabelDecorator {

	@Override
	public void decorate(Object element, IDecoration decoration) {
		ImageDescriptor overlay = getOverlay(element);
		if (overlay != null) {
			decoration.addOverlay(overlay, IDecoration.BOTTOM_LEFT);
		}
	}

	private ImageDescriptor getOverlay(Object element) {
		if (element instanceof IResource) {
			IResource resource = (IResource) element;
			IProject project = resource.getProject();
			if (project != null) {
				IScriptProject scriptProject = DLTKCore.create(project);
				if (scriptProject != null && scriptProject.isOnBuildpath(resource)) {
					IModelElement modelElement = scriptProject.getProjectFragment(resource);
					if (modelElement instanceof ArchiveProjectFragment) {
						return PHPPluginImages.DESC_OVR_LIBRARY;
					}
				}
			}
		}
		return null;
	}

	@Override
	public void addListener(ILabelProviderListener listener) {

	}

	@Override
	public void dispose() {

	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {
	}

}
