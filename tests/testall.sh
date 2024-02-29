# for debug output
#set -x

LANGS=("ruby" "cpp" "golang" "java" "kotlin") # "c"
EXES=(
  'bazel-bin/ruby/sslc.cmd'
  'bazel-bin/cpp/sslc.exe'
  'bazel-bin/golang/sslc_/sslc.exe'
  'bazel-bin/java/com/plasstech/lang/ssl/sslc.exe'
  'bazel-bin/kotlin/sslc.exe'
)
#'bazel-bin/c/sslc.exe'

# Find all binary rules and remove the leadin g//
bazel query 'kind(".*binary", ...)' | sed 's.//..' | xargs bazel build

RED='\033[0;31m'
GREEN='\033[1;32m'
BLUE='\033[1;34m'
NC='\033[0m' # No Color

# These three are the inputs to runone
source=''
lang=''
exe=''

runone() {
  BASE=`basename -s .ssl $source`
  rm -f $lang.out $BASE.*
  $exe < $source > $BASE.asm && nasm -fwin64 $BASE.asm && gcc $BASE.obj -o $BASE.exe && ./$BASE.exe > $lang.out
}


# Run all languages for the input file 'source'
testone_source() {
  # Arbitraraily consider Python the golden
  echo -n "python:"
  lang='python'
  exe='bazel-bin/python/sslc.exe'
  runone
  echo -n -e " ${BLUE}RUN${NC}"
  for i in ${!EXES[@]}; do
    lang=${LANGS[$i]}
    exe=${EXES[$i]}
    echo -n ", $lang: "
    runone
    diff $lang.out python.out && echo -n -e "${GREEN}PASS${NC}" || (echo -n -e "${RED}FAIL${NC}" && exit -4) || exit
  done
}

for source in `ls samples/*.ssl`; do
  echo ""
  echo -n "Testing ${source}: "
  testone_source
done
