/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.guvnor.common.services.project.client.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.guvnor.common.services.project.client.resources.ProjectResources;
import org.guvnor.common.services.project.client.resources.i18n.ProjectConstants;
import org.guvnor.common.services.project.model.Project;
import org.guvnor.common.services.project.service.ProjectSearchService;
import org.jboss.errai.common.client.api.Caller;
import org.uberfire.security.Resource;
import org.uberfire.security.ResourceAction;
import org.uberfire.security.authz.Permission;
import org.uberfire.security.authz.PermissionManager;
import org.uberfire.security.client.authz.tree.LoadCallback;
import org.uberfire.security.client.authz.tree.LoadOptions;
import org.uberfire.security.client.authz.tree.PermissionNode;
import org.uberfire.security.client.authz.tree.PermissionTreeProvider;
import org.uberfire.security.client.authz.tree.impl.PermissionLeafNode;
import org.uberfire.security.client.authz.tree.impl.PermissionResourceNode;

import static org.guvnor.common.services.project.security.ProjectAction.*;

/**
 * The {@link PermissionTreeProvider} plugin that brings {@link Project} permissions into the ACL editor
 */
@ApplicationScoped
public class ProjectTreeProvider implements PermissionTreeProvider {

    private ProjectConstants i18n = ProjectResources.CONSTANTS;
    private PermissionManager permissionManager;
    private int rootNodePosition = 0;
    private Caller<ProjectSearchService> searchService;

    public ProjectTreeProvider() {
    }

    @Inject
    public ProjectTreeProvider(PermissionManager permissionManager, Caller<ProjectSearchService> searchService) {
        this.permissionManager = permissionManager;
        this.searchService = searchService;
    }

    public int getRootNodePosition() {
        return rootNodePosition;
    }

    public void setRootNodePosition(int rootNodePosition) {
        this.rootNodePosition = rootNodePosition;
    }

    @Override
    public PermissionNode buildRootNode() {
        PermissionResourceNode rootNode = new PermissionResourceNode(i18n.ProjectResource(), this);
        rootNode.setNodeName(i18n.ProjectsNode());
        rootNode.setPositionInTree(rootNodePosition);
        rootNode.addPermission(newPermission(READ), i18n.ProjectActionRead());
        rootNode.addPermission(newPermission(UPDATE), i18n.ProjectActionUpdate());
        rootNode.addPermission(newPermission(DELETE), i18n.ProjectActionDelete());
        rootNode.addPermission(newPermission(BUILD), i18n.ProjectActionBuild());
        rootNode.addPermission(newPermission(CREATE), i18n.ProjectActionCreate());
        return rootNode;
    }

    private Permission newPermission(ResourceAction action) {
        return permissionManager.createPermission(Project.RESOURCE_TYPE, action, true);
    }

    private Permission newPermission(Resource resource, ResourceAction action) {
        return permissionManager.createPermission(resource, action, true);
    }

    @Override
    public void loadChildren(PermissionNode parent, LoadOptions options, LoadCallback callback) {
        Collection<String> resourceIds = options.getResourceIds();
        int maxNodes = options.getMaxNodes();

        if (searchService != null) {
            if (resourceIds != null) {
                searchService.call((Collection<Project> projects) -> {
                    List<PermissionNode> children = buildPermissionNodes(projects);
                    callback.afterLoad(children);
                }).searchById(resourceIds);
            }
            else {
                String namePattern = options.getNodeNamePattern();
                searchService.call((Collection<Project> projects) -> {
                    List<PermissionNode> children = buildPermissionNodes(projects);
                    callback.afterLoad(children);
                }).searchByName(namePattern, maxNodes, false);
            }
        } else {
            callback.afterLoad(Collections.emptyList());
        }
    }

    private List<PermissionNode> buildPermissionNodes(Collection<Project> projects) {
        List<PermissionNode> nodes = new ArrayList<>();
        for (Project p : projects) {
            nodes.add(toPermissionNode(p));
        }
        return nodes;
    }

    private PermissionNode toPermissionNode(Project p) {
        PermissionLeafNode node = new PermissionLeafNode();
        node.setNodeName(p.getProjectName());
        node.addPermission(newPermission(p, READ), i18n.ProjectActionRead());
        node.addPermission(newPermission(p, UPDATE), i18n.ProjectActionUpdate());
        node.addPermission(newPermission(p, DELETE), i18n.ProjectActionDelete());
        node.addPermission(newPermission(p, BUILD), i18n.ProjectActionBuild());
        return node;
    }
}