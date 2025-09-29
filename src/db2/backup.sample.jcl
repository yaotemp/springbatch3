//DB2FULL  JOB (ACCT),'DB2 FULL COPY',CLASS=A,MSGCLASS=X,NOTIFY=&SYSUID
//* ====== Common variable definitions ======
/* SET HLQ=MYDB                /* dataset HLQ                         */
/* SET SUBSYS=DSN1             /* Db2 subsystem ID                    */
/* SET DBNAME=LAB01            /* database name                       */
/* SET TSNAME=TSLAB01          /* tablespace name                     */
/* SET UNIT=SYSDA              /* storage unit                        */
/* SET PRI=10,SEC=1            /* space allocation (cyl)              */
/* SET UID=BKUP                /* utility job UID                     */
/* SET TAG=Y20250929.T120000   /* timestamp tag                       */
/* SET PARALLEL=2              /* parallelism                         */
/* SET COPY1=SYSCOPY           /* primary copy DD name                */
/* SET COPY2=SYSCOPY2          /* secondary copy DD name              */
/* SET RCOPY1=COPYR1           /* remote primary copy DD name         */
/* SET RCOPY2=COPYR2           /* remote secondary copy DD name       */
//*------------------------------------------------------------------*
//* Step: Full copy of tablespace &DBNAME..&TSNAME
//*------------------------------------------------------------------*
//FULLTS  EXEC DSNUPROC,SYSTEM=&SUBSYS,UID='&UID'
//&COPY1  DD DSN=&HLQ..COPY.&TSNAME..FULL.&TAG,
//          DISP=(NEW,CATLG,DELETE),UNIT=&UNIT,
//          SPACE=(CYL,(&PRI,&SEC))
//&COPY2  DD DSN=&HLQ..COPY.&TSNAME..FULL.&TAG..BKP,
//          DISP=(NEW,CATLG,DELETE),UNIT=&UNIT,
//          SPACE=(CYL,(&PRI,&SEC))
//&RCOPY1 DD DSN=&HLQ..RCOPY.&TSNAME..FULL.&TAG,
//          DISP=(NEW,CATLG,DELETE),UNIT=&UNIT,
//          SPACE=(CYL,(&PRI,&SEC))
//&RCOPY2 DD DSN=&HLQ..RCOPY.&TSNAME..FULL.&TAG..BKP,
//          DISP=(NEW,CATLG,DELETE),UNIT=&UNIT,
//          SPACE=(CYL,(&PRI,&SEC))
//SYSPRINT DD SYSOUT=*
//SYSIN    DD *
  COPY TABLESPACE &DBNAME..&TSNAME
       FULL YES                 /* full image copy */
       SHRLEVEL CHANGE           /* allow read/write during copy */
       PARALLEL &PARALLEL
       COPYDDN(&COPY1,&COPY2)
       RECOVERYDDN(&RCOPY1,&RCOPY2)
/*
