# yang-transformer
## Background
In order to accurately customize our products, Huawei uses a large number of deviation statements in Yang files. 
However, many existing Yang parsers have problems in parsing the deviation statements, which are manifested in different
loading sequences and inconsistent parsing results. For example, if we first parse a not-supported deviation and then 
process the node augment to the path, an error will be reported that the path cannot be found. 
NSO, Pyang and many other Yang parsers in the industry all have this issue. Especially in the NSO integration, 
compilation failures often occur because the file loading order cannot be controlled. Customers often think that 
the Yang files provided by Huawei have some errors.

And, in some scenarios, users may want to only reserve a subset of yang files what the device supports. Because they don't want to see some data what they don't care about.

Therefore, it is necessary to develop a tool to remove all deviation statements and change the target nodes to effective statements. And this tool can tailor the yang files what users don't want to reserve. 

Yang transformer, it's a suitable tool. It's based on [YangKit](https://github.com/yang-central/yangkit),yangkit has a very good Yang parser, it can parse deviations correctly regardless the order.

## Specification
* delete all deviations.
* expand all uses and delete all groupings.
* delete all not-supported schema nodes.
* change all schema nodes what deviations are applied to.
* add new import or include if necessary.
  For example, a typedef used by a grouping defined in other module(bar), and this grouping(uses) is expanded in this module(foo),
  if the bar module is not in import, a new import(bar) should be added to foo module.
* delete all un-used typedefs,imports and modules
* support only reserve huawei native yang modules.
* support reserve user specified yang modules 

## Installation
### Prerequisites
* JDK or JRE 1.8 or above

### Obtain code
git clone https://github.com/HuaweiDatacomm/yang-transformer.git

### build code
mvn clean install

it will generate yang-transformer-1.0-SNAPSHOT.jar and libs directory under the directory target

copy yang-transformer-1.0-SNAPSHOT.jar and libs to anywhere in your computer.

## Usage:
java -jar yang-transformer-1.0-SNAPSHOT.jar -src <_source directory_> -out <_transformed directory_> 
[ -no-deviations | reserve-deviations] [-reserve { --only-huawei-native | {--module | --namespace} 
{match _regex_ except _regex_...}...]

### **Parameters**
1. src: mandatory, source directory for yang files
2. out: mandatory, the output directory for transformed yang files
3. deviations: optional, no-deviations is default. no-deviations means all deviations will be deleted,and the target nodes will be changed to effective statements. reserve-deviations means all deviation will be reserved, and the target node will not be changed.
4. reserve: optional, specify the options of reservation of yang files.
   1. [x] only-huawei-native: reserve all huawei native yang files and necessary dependent yang files. other Yang files will be tailored.
   2. [x] customization options:
      1. [x] reserve type: one of module or namespace. module means the conditions will be applied to module name, namespace means the conditions will be applied to module's namespace.
      2. [x] match: specify the match condition, can occur multiple times. Its argument is a regular expression. The candidate Yang files which the field what the reserve type indicates starts with the regular expression will be reserved(if no except option).
         1. [x] except: specify the invert match condition, can occur multiple times. Its argument is a regular expression. If a yang file match the match regex,but also match the except regex, it will not be reserved.


### Example:
download 8.21.0 versions yang files of network-router from https://github.com/Huawei/yang
and copy to example/yang

cd example and execute the command:

java -jar yang-transformer-1.0-SNAPSHOT.jar -src yang -out yang_trans

The transformed yang files will occur in yang_trans directory.

You can use [yang-comparator](https://github.com/HuaweiDatacomm/yang-comparator) tool to get the difference of the two directories and to see whether the schema nodes are changed ( it should be no any change.).

java -jar yang-transformer-1.0-SNAPSHOT.jar -src yang -out yang_trans1 -reserve --only-huawei-native

The transformed yang files will occur in yang_trans1 directory. only huawei native yang files and necessary dependent yang files are reserved.

java -jar yang-transformer-1.0-SNAPSHOT.jar -src yang -out yang_trans2 -reserve --module match huawei except huawei-ietf huawei-openconfig match openconfig-telemetry match huawei-openconfig-telemetry-ext

The transformed yang files will occur in yang_trans2 directory. 






