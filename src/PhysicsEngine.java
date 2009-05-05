/**
 * Copyright 2008 code_swarm project team
 *
 * This file is part of code_swarm.
 *
 * code_swarm is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * code_swarm is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with code_swarm.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.Properties;
import java.util.LinkedList;
import javax.vecmath.Vector2f;

/**
 * Abstract interface of any code_swarm physical engine.
 *
 * @note Need to be derived to define force calculation algorithms between Nodes
 * @note Need to use the constructor to apply some configuration options
 * 
 * @note For portability, no Processing library should be use there, only standard Java packages
 */
public interface PhysicsEngine
{
  /**
   * Initialize the Physical Engine
   * @param c The code_swarm object that is using us.
   * @param p Properties file
   */
  public void setup (code_swarm c, Properties p);
  
  /**
   * Method that allows Physics Engine to initialize the Frame
   * 
   */
  public void initializeFrame();
  
  /**
   * Method that allows Physics Engine to finalize the Frame
   * 
   */
  public void finalizeFrame();
  
  /**
   * Method that allows Physics Engine to modify Speed / Position during the relax phase.
   * 
   * @param edges the nodes to which the force apply
   * 
   * @return LinkedList<code_swarm.Edge> which is the list of edges which are still active/living
   *           after the relaxing.
   *
   * @Note Standard physics is "Position Variation = Speed x Duration" with a convention of "Duration=1" between to frames
   */
  public LinkedList<code_swarm.Edge> onRelaxEdges(LinkedList<code_swarm.Edge> edges);
  
  /**
   * Method that allows Physics Engine to modify Speed / Position during the relax phase.
   * 
   * @param fNodes the nodes to which the force apply
   * 
   * @return LinkedList<code_swarm.FileNode> which is the list of FileNodes which are still active/living
   *           after the relaxing.
   *
   * @Note Standard physics is "Position Variation = Speed x Duration" with a convention of "Duration=1" between to frames
   */
  public LinkedList<code_swarm.FileNode> onRelaxNodes(LinkedList<code_swarm.FileNode> fNodes);
  
  /**
   * Method that allows Physics Engine to modify Speed / Position during the relax phase.
   * 
   * @param pNodes the nodes to which the force apply
   *
   * @return LinkedList<code_swarm.PersonNode> which is the list of PersonNodes which are still active/living
   *           after the relaxing.
   * 
   * @Note Standard physics is "Position Variation = Speed x Duration" with a convention of "Duration=1" between to frames
   */
  public LinkedList<code_swarm.PersonNode> onRelaxPeople(LinkedList<code_swarm.PersonNode> pNodes);
  
  /**
   * Method that allows Physics Engine to modify Speed / Position during the update phase.
   * 
   * @param edges the nodes to which the force apply
   * 
   * @return LinkedList<code_swarm.Edge> which is the list of edges which are still active/living
   *           after the update.
   * 
   * @Note Standard physics is "Position Variation = Speed x Duration" with a convention of "Duration=1" between to frames
   */
  public LinkedList<code_swarm.Edge> onUpdateEdges(LinkedList<code_swarm.Edge> edges);
  
  /**
   * Method that allows Physics Engine to modify Speed / Position during the update phase.
   * 
   * @param fNode the node to which the force apply
   * 
   * @return LinkedList<code_swarm.FileNode> which is the list of FileNodes which are still active/living
   *           after the update.
   * 
   * @Note Standard physics is "Position Variation = Speed x Duration" with a convention of "Duration=1" between to frames
   */
  public LinkedList<code_swarm.FileNode> onUpdateNodes(LinkedList<code_swarm.FileNode> fNodes);
  
  /**
   * Method that allows Physics Engine to modify Speed / Position during the update phase.
   * 
   * @param pNode the node to which the force apply
   * 
   * @return LinkedList<code_swarm.PersonNode> which is the list of PersonNodes which are still active/living
   *           after the update.
   * 
   * @Note Standard physics is "Position Variation = Speed x Duration" with a convention of "Duration=1" between to frames
   */
  public LinkedList<code_swarm.PersonNode> onUpdatePeople(LinkedList<code_swarm.PersonNode> pNodes);
  
  /**
   * 
   * @return Vector2f vector holding the starting location for a Person Node
   */
  public Vector2f pStartLocation();
  
  /**
   * 
   * @return Vector2f vector holding the starting location for a File Node
   */
  public Vector2f fStartLocation();
  
  /**
   * 
   * @return Vector2f vector holding the starting velocity for a Person Node
   */
  public Vector2f pStartVelocity(float mass);
  
  /**
   * 
   * @return Vector2f vector holding the starting velocity for a File Node
   */
  public Vector2f fStartVelocity(float mass);
  
}

