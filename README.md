# Cooperative BDI agents in Tileworld

How does a team decide which tile to collect when its members have limited energy and the world changes around them? This Java simulation uses Repast Simphony to study coordination in a dynamic grid. Agents locate tiles and holes, carry tiles to targets, avoid obstacles, and visit energy stations. A shared blackboard gives agents access to targets discovered by their teammates.

## Follow one decision

Start in [Agent.java](src/multiagent_tileworld_plus/Agent.java). The tile and hole selection methods identify targets, while the movement methods determine the next grid step. [Blackboard.java](src/multiagent_tileworld_plus/Blackboard.java) implements the shared information store. [DynamicEnvironmentManager.java](src/multiagent_tileworld_plus/DynamicEnvironmentManager.java) controls changes to the environment.

## Run the simulation

Import the repository as an existing Eclipse project in a Repast Simphony installation. Open the launch configuration under [launchers](launchers/) and select the included [Repast scenario](multiagent_tileworld_plus.rs/) if prompted. Initialize the model, then start the simulation from the Repast toolbar.

The [experimental report](report.pdf) describes the BDI design, evaluation setup, and observed behavior.
