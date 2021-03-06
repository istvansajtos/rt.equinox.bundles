/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.region;

import static org.eclipse.equinox.region.RegionFilter.VISIBLE_OSGI_SERVICE_NAMESPACE;
import static org.eclipse.equinox.region.RegionFilter.VISIBLE_SERVICE_NAMESPACE;

import java.util.*;
import org.eclipse.equinox.region.RegionFilter;
import org.eclipse.equinox.region.RegionFilterBuilder;
import org.osgi.framework.*;

public final class StandardRegionFilterBuilder implements RegionFilterBuilder {

	private final static String ALL_SPEC = "(|(!(all=*))(all=*))"; //$NON-NLS-1$

	private final static Filter ALL;

	static {
		try {
			ALL = FrameworkUtil.createFilter(ALL_SPEC);
		} catch (InvalidSyntaxException e) {
			// should never happen!
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	private final Object monitor = new Object();

	private final Map<String, Collection<Filter>> policy = new HashMap<String, Collection<Filter>>();

	@SuppressWarnings("deprecation")
	public RegionFilterBuilder allow(String namespace, String filter) throws InvalidSyntaxException {
		if (namespace == null)
			throw new IllegalArgumentException("The namespace must not be null."); //$NON-NLS-1$
		if (filter == null)
			throw new IllegalArgumentException("The filter must not be null."); //$NON-NLS-1$
		synchronized (this.monitor) {
			Collection<Filter> namespaceFilters = policy.get(namespace);
			if (namespaceFilters == null) {
				// use set to avoid duplicates
				namespaceFilters = new LinkedHashSet<Filter>();
				policy.put(namespace, namespaceFilters);
			}
			// TODO need to use BundleContext.createFilter here
			namespaceFilters.add(FrameworkUtil.createFilter(filter));
		}
		if (VISIBLE_SERVICE_NAMESPACE.equals(namespace)) {
			// alias the deprecated namespace to osgi.service
			allow(VISIBLE_OSGI_SERVICE_NAMESPACE, filter);
		}
		return this;
	}

	@SuppressWarnings("deprecation")
	public RegionFilterBuilder allowAll(String namespace) {
		if (namespace == null)
			throw new IllegalArgumentException("The namespace must not be null."); //$NON-NLS-1$
		synchronized (this.monitor) {
			Collection<Filter> namespaceFilters = policy.get(namespace);
			if (namespaceFilters == null) {
				// use set to avoid duplicates
				namespaceFilters = new LinkedHashSet<Filter>();
				policy.put(namespace, namespaceFilters);
			}
			// remove any other filters since this will override them all.
			namespaceFilters.clear();
			namespaceFilters.add(ALL);
		}
		if (VISIBLE_SERVICE_NAMESPACE.equals(namespace)) {
			// alias the deprecated namespace to osgi.service
			allowAll(VISIBLE_OSGI_SERVICE_NAMESPACE);
		}
		return this;
	}

	public RegionFilter build() {
		synchronized (this.monitor) {
			return new StandardRegionFilter(policy);
		}
	}
}
