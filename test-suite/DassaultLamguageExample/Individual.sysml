package Individual {
    view individuals : DS_Views::SymbolicViews::gv;
    package 'IT company' {
        private import Time::**;
        part def Person :> 'Actual Element';
        part def 'Actual Element' {
            attribute startDate;
        }
        part def Organization :> 'Actual Element';
        part def Post :> 'Actual Element' {
            ref part fillPost [0..1] : Person;
        }
        part def 'IT Company' :> Organization {
            part groupLeader : 'Group Leader';
            part consultant : Consultant [10];
            part employee [*] : Person;
            part ceo : CEO;
            part department : Department;
        }
        part def CEO :> Post {
            constraint responsibility {}
            attribute salary;
        }
        part def 'Group Leader' :> Post;
        part def Consultant :> Post;
        part def Department :> Organization;
        individual part def 'No Magic' :> 'IT Company' {
            individual part :>> ceo : 'Ceo (position)' {
                timeslice part y2005 : 'Ceo (position)' {
                    individual part :>> fillPost : Matthew;
                    attribute :>> startDate : Iso8601DateTime {
                        :>> val = "2005-02-07";
                    }
                }
            }
            individual part consultalt1 :> consultant : 'Consultant (position)' {
                individual part :>> fillPost : Andrew;
            }
        }
        individual part def Matthew :> Person;
        individual part def 'Ceo (position)' :> CEO;
        individual part def Andrew :> Person;
        individual part def 'Consultant (position)' :> Consultant;
        comment about 'No Magic'::consultalt1 /* If multiplicity is more than 1, subset should be used instead of redefinition, in this case company has 10 consultants*/
        comment about Andrew, Matthew, 'Consultant (position)',
        'Ceo (position)'
        /* Individual is represented as a definition with specialization (can be optional, if there is no corresponding definition). 
         * The individual is a single real or perceived object with a unique identity.*/

        comment about 'No Magic'::consultalt1::fillPost,
        'No Magic'::ceo::y2005::fillPost /* Individual usage is typed by definition showing that for some period of time that individual plays this role*/

        comment about 'No Magic'::ceo /* Redefinition of inherited features is performed and marked as an individual.*/
        comment about 'No Magic'::ceo::y2005 /* The position of CEO is time-sliced into years, for example in the year 2005 this position was performed by Matthew.*/
    }
}
