package org.boxpicbak;

import java.util.ArrayList;
import java.util.List;

public class PicBakBoxNode {
    public enum eNodeType {
        folder,
        file
    }
    
    private List<PicBakBoxNode> children = new ArrayList<PicBakBoxNode>();
    private String nodeID;
    private String nodeName;
    private String nodeSHA1;
    private eNodeType nodeType;
    
    public String getNodeID() {
        return nodeID;
    }
    public void setNodeID(String nodeID) {
        this.nodeID = nodeID;
    }
    public String getNodeName() {
        return nodeName;
    }
    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }
    public List<PicBakBoxNode> getChildren() {
        return children;
    }
    public eNodeType getNodeType() {
        return nodeType;
    }
    public void setNodeType(eNodeType nodeType) {
        this.nodeType = nodeType;
    }
    public String getNodeSHA1() {
        return nodeSHA1;
    }
    public void setNodeSHA1(String nodeSHA1) {
        this.nodeSHA1 = nodeSHA1;
    }
    @Override
    public String toString() {
        return "PicBakBoxNode [children=" + children + ", nodeID=" + nodeID + ", nodeName=" + nodeName + ", nodeSHA1=" + nodeSHA1 + ", nodeType=" + nodeType + "]";
    }
}
