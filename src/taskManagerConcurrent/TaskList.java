package taskManagerConcurrent;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;

/**
 * JAXB class - task wrapper This class is based on a code snippet by Rao and
 * follows his structure with public fields/no explicit constructor
 * 
 * @author BieberFever (based on codesnippet by rao)
 */
@XmlRootElement(name = "tasks")
@XmlSeeAlso(Task.class)
public class TaskList implements Serializable {
  private static final long serialVersionUID = 7526472295622776147L;

  protected List<Task> list = new ArrayList<Task>();

  public TaskList() {}

  public TaskList(List<Task> list) {
    this.list = list;
  }

  public boolean conditionsCheck(Task task) {
    boolean condition = true;

    String cons = task.conditions;
    if (cons != null && !cons.isEmpty()) { // has conditions
      String[] conditions = cons.split(",");
      for (String s : conditions) {
        s = s.trim();
        if (this.getTask(s).status.equals("not-executed")) {
          condition = false;
        }
      }
    }
    return condition;
  }

  public Task getTask(String id) {
    return getTaskSQESA(id);
  }

  private Task getTaskSQESA(String id) {
    // Super Quickly Efficient Search Algorithm (SQESA)
    Task task = null;
    for (Task t : list) {
      if (t.id.equals(id)) {
        if (!t.status.equals("executed")) task = t;
        break;
      }
    }
    return task;
  }

  @XmlElement(name = "task")
  public List<Task> getList() {
    return list;
  }
}
