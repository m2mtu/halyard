/*
 * Copyright 2018 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
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
 *
 *
 */

package com.netflix.spinnaker.halyard.deploy.spinnaker.v1.service.distributed.kubernetes.v2;

import com.netflix.spinnaker.halyard.config.model.v1.node.DeploymentConfiguration;
import com.netflix.spinnaker.halyard.deploy.spinnaker.v1.SpinnakerRuntimeSettings;
import com.netflix.spinnaker.halyard.deploy.spinnaker.v1.profile.Profile;
import com.netflix.spinnaker.halyard.deploy.spinnaker.v1.profile.ShutdownScriptProfileFactoryBuilder;
import com.netflix.spinnaker.halyard.deploy.spinnaker.v1.service.OrcaService;
import com.netflix.spinnaker.halyard.deploy.spinnaker.v1.service.ServiceSettings;
import lombok.experimental.Delegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class KubernetesV2OrcaService extends OrcaService implements KubernetesV2Service<OrcaService.Orca> {
  @Delegate
  @Autowired
  KubernetesV2ServiceDelegate serviceDelegate;

  @Autowired
  ShutdownScriptProfileFactoryBuilder shutdownScriptProfileFactoryBuilder;

  @Override
  public List<Profile> getProfiles(DeploymentConfiguration deploymentConfiguration, SpinnakerRuntimeSettings endpoints) {
    List<Profile> profiles = super.getProfiles(deploymentConfiguration, endpoints);
    ServiceSettings settings = endpoints.getServiceSettings(getService());
    profiles.add(shutdownScriptProfileFactoryBuilder.build(
        "wget --header=\"content-type: application/json\"  --post-data='{\"enabled\": \"false\"}' "
            + settings.getScheme() + "://localhost:" + settings.getPort() + "/admin/instance/enabled\n\n"
            + "sleep " + terminationGracePeriodSeconds() / 2 + "\n",
        getArtifact()
    ).getProfile("orca/shutdown.sh", shutdownScriptFile(), deploymentConfiguration, endpoints));
    return profiles;
  }

  @Override
  public boolean hasPreStopCommand() {
    return true;
  }

  @Override
  public ServiceSettings defaultServiceSettings() {
    return new Settings();
  }
}
