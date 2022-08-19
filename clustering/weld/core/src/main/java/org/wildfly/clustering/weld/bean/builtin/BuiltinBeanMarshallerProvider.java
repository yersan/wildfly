/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.clustering.weld.bean.builtin;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.InjectionPoint;

import org.jboss.weld.bean.builtin.InstanceImpl;
import org.jboss.weld.manager.BeanManagerImpl;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshallerProvider;
import org.wildfly.clustering.marshalling.protostream.reflect.TernaryMethodMarshaller;

/**
 * @author Paul Ferraro
 */
public enum BuiltinBeanMarshallerProvider implements ProtoStreamMarshallerProvider {

    @SuppressWarnings("unchecked")
    INSTANCE_IMPL(new TernaryMethodMarshaller<>(InstanceImpl.class, InjectionPoint.class, CreationalContext.class, BeanManagerImpl.class, (injectionPoint, context, manager) -> InstanceImpl.of(injectionPoint, context, manager))),
    ;
    private final ProtoStreamMarshaller<?> marshaller;

    BuiltinBeanMarshallerProvider(ProtoStreamMarshaller<?> marshaller) {
        this.marshaller = marshaller;
    }

    @Override
    public ProtoStreamMarshaller<?> getMarshaller() {
        return this.marshaller;
    }
}