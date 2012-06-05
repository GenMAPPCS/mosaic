/*******************************************************************************
 * Copyright 2011 Chao Zhang
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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
 ******************************************************************************/
package org.nrnb.mosaic.utils;

public class MosaicStaticValues {
    public static final String BP_ATTNAME = "annotation.GO BIOLOGICAL_PROCESS";
    public static final String CC_ATTNAME = "annotation.GO CELLULAR_COMPONENT";
    public static final String MF_ATTNAME = "annotation.GO MOLECULAR_FUNCTION";
//    public static final String BP_ATTNAME = "annotation.GO BP_MOSAIC";
//    public static final String CC_ATTNAME = "annotation.GO CC_MOSAIC";
//    public static final String MF_ATTNAME = "annotation.GO MF_MOSAIC";
    public static final String CC_CODE = "Tc";
    public static final String BP_CODE = "Tb";
    public static final String MF_CODE = "Tm";
    public static String[] speciesList = {"Human", "Mouse", "Yeast", "Arabidopsis"};
    //public static final String[] speciesList = {"Human", "Mouse", "Yeast", "Fruitfly", "Celegans","Zebrafish"};
    //public static final String bridgedbSpecieslist = "http://svn.bigcat.unimaas.nl/bridgedb/trunk/org.bridgedb.bio/resources/org/bridgedb/bio/organisms.txt";
    public static final String supportedSpecieslist = "/supported_species.tab";
    public static final String bridgedbSpecieslist = "/resources/organisms.txt";
    public static final String genmappcsDatabaseDir = "http://genmapp.org/genmappcs/databases/";
    public static final String bridgedbDerbyDir = "http://bridgedb.org/data/gene_database/";
    //public static final String BP_GO_PathFile = "/resources/annotation.GO BIOLOGICAL_PROCESS";
    //public static final String CC_GO_PathFile = "/resources/annotation.GO CELLULAR_COMPONENT";
    //public static final String MF_GO_PathFile = "/resources/annotation.GO MOLECULAR_FUNCTION";
    public static final String BP_GO_PathFile = "/resources/built_go_paths.txt";
    public static final String CC_GO_PathFile = "/resources/built_go_paths.txt";
    public static final String MF_GO_PathFile = "/resources/built_go_paths.txt";
    //public static final String GO_DescFile = "/resources/gene-ontology.txt";
    public static final String GO_DescFile = "/resources/go_annotations.txt";
}
