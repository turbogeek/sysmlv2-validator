library package DiagramInterchange {
    doc /* Model library to support standardized interchange of diagrams (aka framed views). */
    private import ScalarValues::*;
    private import Views::*;
    abstract view def ElementView;
    view def NodeView :> ElementView {
        render rendering : NodeRendering;
    }
    view def EdgeView :> ElementView {
        ref view sourceView : ElementView;
        ref view targetView : ElementView;
        render rendering : EdgeRendering;
    }
    abstract rendering def NodeRendering :> GraphicalRendering {
        doc
        /* Rendering of a model element as a node.
         *
         * width : width of node in user units.
         * height : height of node in user units.
         */
        attribute width : Real default 0.0;
        attribute height : Real default 0.0;
    }
    rendering asRegularNode : NodeRendering {
        doc
        /* x, y : position of top-left corner of the node in coordinate frame of the owning node.
         * nodeKind : node shape to be rendered.
         */
        attribute x : Real default 0.0;
        attribute y : Real default 0.0;
    }
    rendering asBorderNode : NodeRendering {
        doc
        /* xc, yc : position of the centerline of the border node on given border of the owning node.
         * nodeKind : node shape to be rendered.
         * onBorder : border of the owning node on which this node is placed.
         */
        attribute xc : Real default 0.0;
        attribute yc : Real default 0.0;
        attribute onBorder : BorderKind;
    }
    abstract rendering def EdgeRendering :> GraphicalRendering {
        doc
        /* Rendering of a relationship or relationship-like element as an edge.
         *
         * sourceOnBorder : border on sourceView node
         * xs, ys : start position of the edge in coordinate-frame of relationship source node.
         * targetOnBorder : border on targetView node
         * xt, yt : end position of the edge in coordinate-frame of relationship target node.
         * wayPoints : optional sequence of x, y intermediate waypoint positions for routing of the edge.
         * labels : optional set of text labels to be rendered along the edge.
         */
        attribute sourceOnBorder : BorderKind [0..1] default null;
        attribute xs : Real default 0.0;
        attribute ys : Real default 0.0;
        attribute targetOnBorder : BorderKind [0..1] default null;
        attribute xt : Real default 0.0;
        attribute yt : Real default 0.0;
        attribute wayPoints : Real [0..*] ordered default null;
    }
    rendering asEdge : EdgeRendering {
        doc /* Rendering of a relationship or relationship-like element as an edge.
         */
    }
    enum def <BK> BorderKind {
        doc /* Enumeration of possible asBorderNode::onBorder values. */
        top;
        right;
        bottom;
        left;
    }
}
