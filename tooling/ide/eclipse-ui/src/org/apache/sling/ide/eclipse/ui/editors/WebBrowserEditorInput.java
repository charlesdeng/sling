/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.ide.eclipse.ui.editors;

import org.apache.sling.ide.eclipse.ui.internal.SharedImages;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

public class WebBrowserEditorInput implements IEditorInput {
	private final String url;

	public WebBrowserEditorInput(String url) {
		this.url = url;
	}

	@Override
	public Object getAdapter(Class adapter) {
		return null;
	}

	@Override
	public String getToolTipText() {
		return getUrl();
	}

	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	@Override
	public String getName() {
		return "the name";
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return SharedImages.SLING_ICON;
	}

	@Override
	public boolean exists() {
		return true;
	}

	public String getUrl() {
		return url;
	}
}