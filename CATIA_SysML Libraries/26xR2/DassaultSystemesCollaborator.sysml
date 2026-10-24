library package <DS_Collaborator> DassaultSystemesCollaborator {
    private import ScalarValues::*;
    metadata def CollaboratorComment {
        :>> annotatedElement : KerML::Root::Comment;
        attribute author : String [1];
        attribute date : String [1];
        attribute highPriority : Boolean [1] default false;
        attribute isResolved : Boolean [1];
        attribute readBy : String [0..*];
        attribute lastModifiedDate [0..1] : String;
    }
}
