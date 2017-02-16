/*******************************************************************************
 * Copyright (c) 2009, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Zend Technologies
 *     Dawid Pakuła [469503]
 *******************************************************************************/
package org.eclipse.php.internal.ui.explorer;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.dltk.core.*;
import org.eclipse.dltk.core.environment.EnvironmentPathUtils;
import org.eclipse.dltk.internal.core.ArchiveProjectFragment;
import org.eclipse.dltk.internal.core.ExternalProjectFragment;
import org.eclipse.dltk.internal.ui.navigator.ScriptExplorerContentProvider;
import org.eclipse.dltk.internal.ui.navigator.ScriptExplorerLabelProvider;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.php.core.libfolders.LibraryFolderManager;
import org.eclipse.php.internal.core.includepath.IncludePath;
import org.eclipse.php.internal.core.language.LanguageModelInitializer;
import org.eclipse.php.internal.ui.util.LabelProviderUtil;
import org.eclipse.php.internal.ui.util.PHPPluginImages;
import org.eclipse.swt.graphics.Image;

/**
 * 
 * @author apeled, nirc
 * 
 */
public class PHPExplorerLabelProvider extends ScriptExplorerLabelProvider {

	public PHPExplorerLabelProvider(ScriptExplorerContentProvider cp, IPreferenceStore store) {
		super(cp, store);
	}

	@Override
	public Image getImage(Object element) {
		IModelElement modelElement = null;
		if (element instanceof ArchiveProjectFragment) {
			return PHPPluginImages.get(PHPPluginImages.IMG_OBJS_EXTJAR);
		}
		if (element instanceof ExternalProjectFragment) {
			ExternalProjectFragment fragment = (ExternalProjectFragment) element;
			String name = LanguageModelInitializer.getPathName(EnvironmentPathUtils.getLocalPath(fragment.getPath()));
			if (name != null) {
				return PHPPluginImages.get(PHPPluginImages.IMG_OBJS_LIBRARY);
			}
			return PHPPluginImages.get(PHPPluginImages.IMG_OBJS_EXTSRC);
		}

		if (element instanceof IncludePath) {
			Object entry = ((IncludePath) element).getEntry();

			// An included PHP project
			if (entry instanceof IBuildpathEntry) {
				int entryKind = ((IBuildpathEntry) entry).getEntryKind();
				if (entryKind == IBuildpathEntry.BPE_PROJECT) {
					return PHPPluginImages.get(PHPPluginImages.IMG_OBJS_PHP_PROJECT);

				}
				// A library
				if (entryKind == IBuildpathEntry.BPE_LIBRARY) {
					return this.getImage(getProjectFragment((IncludePath) element));
				}
				if (entryKind == IBuildpathEntry.BPE_CONTAINER) {
					return PHPPluginImages.get(PHPPluginImages.IMG_OBJS_LIBRARY);
				}
			}

			if (entry instanceof ExternalProjectFragment) {
				return PHPPluginImages.get(PHPPluginImages.IMG_OBJS_LIBRARY);
			}

			// Folder in the include path, should have same image as in the PHP
			// Explorer .
			if (entry instanceof IFolder) {
				IModelElement createdScriptFolder = DLTKCore.create((IFolder) entry);
				if (null == createdScriptFolder)
					return getImage(entry);
				return getImage(createdScriptFolder);
			}

			if (entry instanceof IResource) {
				return (getImage((IResource) entry));

			}
			return null;
		}

		if (element instanceof IResource) {
			modelElement = DLTKCore.create((IResource) element);
		} else if (element instanceof IModelElement) {
			modelElement = (IModelElement) element;
		}

		if (modelElement != null) {
			IScriptProject project = modelElement.getScriptProject();
			if (!project.isOnBuildpath(modelElement)) {// not in build path,
				// hence: hollow,
				// non-pakg icons
				if (modelElement.getElementType() == IModelElement.SOURCE_MODULE)
					return PHPPluginImages.get(PHPPluginImages.IMG_OBJS_CUNIT_RESOURCE);
				if (modelElement.getElementType() == IModelElement.PROJECT_FRAGMENT
						|| modelElement.getElementType() == IModelElement.SCRIPT_FOLDER)
					return PHPPluginImages.get(PHPPluginImages.IMG_OBJS_PHP_FOLDER);
			} else {// in build path ...
				if (modelElement.getElementType() == IModelElement.PROJECT_FRAGMENT
						|| modelElement.getElementType() == IModelElement.SCRIPT_FOLDER || element instanceof IFolder) {
					LibraryFolderManager lfm = LibraryFolderManager.getInstance();
					if (lfm.isInLibraryFolder(modelElement.getResource())) {
						return PHPPluginImages.get(PHPPluginImages.IMG_OBJS_PHP_LIBFOLDER);
					} else {
						return PHPPluginImages.get(PHPPluginImages.IMG_OBJS_PHPFOLDER_ROOT);
					}
				}
			}
		}

		if (element != null) {
			for (ILabelProvider provider : TreeContentProviderRegistry.getInstance().getLabelProviders()) {
				Image image = provider.getImage(element);

				if (image != null) {
					return image;
				}
			}
		}

		return super.getImage(element);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.dltk.internal.ui.navigator.ScriptExplorerLabelProvider#
	 * getText (java.lang.Object)
	 * 
	 * Override the default text - do not display a full path for a folder
	 */
	@Override
	public String getText(Object element) {
		StyledString label = doGetText(element);
		if (label != null) {
			return label.getString();
		}
		return super.getText(element);
	}

	@Override
	public StyledString getStyledText(Object element) {
		StyledString label = doGetText(element);
		if (label != null) {
			return label;
		}
		StyledString text = super.getStyledText(element);
		if (element instanceof IScriptFolder) {
			return new StyledString(text.getString().replace('/', '\\'));
		}
		return text;
	}

	private StyledString doGetText(Object element) {
		String text = null;
		if (element instanceof ExternalProjectFragment) {
			ExternalProjectFragment fragment = (ExternalProjectFragment) element;
			String name = LanguageModelInitializer.getPathName(EnvironmentPathUtils.getLocalPath(fragment.getPath()));
			if (name != null) {
				return new StyledString(name);
			}
		}
		if (element instanceof IncludePath) {
			Object entry = ((IncludePath) element).getEntry();

			// An included PHP project
			if (entry instanceof IBuildpathEntry) {
				IBuildpathEntry iBuildpathEntry = (IBuildpathEntry) entry;
				if (iBuildpathEntry.getEntryKind() == IBuildpathEntry.BPE_PROJECT) {
					text = iBuildpathEntry.getPath().lastSegment();
				} else if (iBuildpathEntry.getEntryKind() == IBuildpathEntry.BPE_CONTAINER) {
					text = getEntryDescription(element, iBuildpathEntry);
				} else if (iBuildpathEntry.getEntryKind() == IBuildpathEntry.BPE_LIBRARY) {
					return getStyledText(getProjectFragment((IncludePath) element));
				} else {
					String result = LabelProviderUtil.getVariableName(iBuildpathEntry.getPath(),
							iBuildpathEntry.getEntryKind());
					if (result == null) {
						IPath localPath = EnvironmentPathUtils.getLocalPath(iBuildpathEntry.getPath());
						text = localPath.toOSString();
					}
				}
			} else if (entry instanceof ExternalProjectFragment) {
				text = ((ExternalProjectFragment) entry).toStringWithAncestors();
			} else if (entry instanceof IResource) {
				text = (((IResource) entry).getFullPath().toString()).substring(1);
			}
		}

		if (text == null && element != null) {
			for (ILabelProvider provider : TreeContentProviderRegistry.getInstance().getLabelProviders()) {
				String label = provider.getText(element);

				if (label != null) {
					text = label;
					break;
				}
			}
		}
		if (text != null) {
			return new StyledString(text);
		}
		return null;
	}

	/**
	 * @param element
	 * @param iBuildpathEntry
	 * @return the name of the container description
	 */
	private String getEntryDescription(Object element, IBuildpathEntry iBuildpathEntry) {
		IProject project = ((IncludePath) element).getProject();
		IScriptProject scriptProject = DLTKCore.create(project);
		IBuildpathContainer buildpathContainer = null;
		try {
			buildpathContainer = DLTKCore.getBuildpathContainer(iBuildpathEntry.getPath(), scriptProject);
		} catch (ModelException e) {
			// no matching container - return the path
		}
		if (buildpathContainer != null) {
			return buildpathContainer.getDescription();
		}
		return iBuildpathEntry.getPath().toOSString();
	}

	private IProjectFragment getProjectFragment(IncludePath includePath) {
		IScriptProject project = DLTKCore.create(includePath.getProject());
		IBuildpathEntry entry = null;
		if (includePath.getEntry() instanceof IBuildpathEntry) {
			entry = (IBuildpathEntry) includePath.getEntry();
		}
		if (entry == null) {
			return null;
		}
		return project.getProjectFragment(entry.getPath());
	}

}
