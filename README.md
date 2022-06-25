# yang-transformer
## Background
In order to accurately customize our products, Huawei uses a large number of deviation statements in Yang files. 
However, many existing Yang parsers have problems in parsing the deviation statements, which are manifested in different
loading sequences and inconsistent parsing results. For example, if we first parse a not-supported deviation and then 
process the node augment to the path, an error will be reported that the path cannot be found. 
NSO, Pyang and many other Yang parsers in the industry all have this issue. Especially in the NSO integration, 
compilation failures often occur because the file loading order cannot be controlled. Customers often think that 
the Yang files provided by Huawei have some errors.

Therefore, it is necessary to develop a tool to remove all not support deviations and directly delete unsupported nodes 
in the Yang file. This tool can be provided to customers when NSO compilation errors occur.

## Specification
* delete all not-supported deviations.
* expand all uses and delete all groupings.
* delete all not-supported schema nodes.
* add new import or include if necessary.
  For example, a typedef used by a grouping defined in other module(bar), and this grouping(uses) is expanded in this module(foo),
  if the bar module is not in import, a new import(bar) should be added to foo module.
* delete all un-used typedefs,imports

## Installation
### Prerequisites
* JDK or JRE 1.8 or above

### Obtain code
git clone https://github.com/HuaweiDatacomm/yang-transformer.git

### build code
mvn clean install

it will generate yang-transformer-1.0-SNAPSHOT.jar and libs directory under the directory target

copy yang-comparator-1.0-SNAPSHOT.jar and libs to anywhere in your computer.

## Usage:
java -jar yang-transformer-1.0-SNAPSHOT.jar _{source directory} {transformed directory}_

### Example:
download 8.21.0 versions yang files of network-router from https://github.com/Huawei/yang
and copy to example/yang

cd example and execute the command:

java -jar yang-transformer-1.0-SNAPSHOT.jar yang yang_trans

The transformed yang files will occur in yang_trans directory.

You can use [yang-comparator](https://github.com/HuaweiDatacomm/yang-comparator) tool to get the difference of the two directories and to see whether the schema nodes are changed ( it should be no any change.).




