package taskManagerGroupCommunication;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;


/**
 * JAXB class - task wrapper
 * This class is based on a code snippet by Rao and follows his structure with public fields/no explicit constructor
 * @author BieberFever (based on codesnippet by rao)
 */
@XmlRootElement(name = "tasks")
@XmlSeeAlso(Task.class)
public class TaskList implements Serializable {
	private static final long serialVersionUID = 7526472295622776147L;

    protected List<Task> list = new ArrayList<Task> ();
    
    public TaskList(){}

    public TaskList(List<Task> list) {
        this.list = list;
    }

    @XmlElement(name = "task")
    public List<Task> getList() {
        return list;
    }
}
