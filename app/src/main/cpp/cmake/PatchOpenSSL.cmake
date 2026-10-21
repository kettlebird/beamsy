set(OPENSSL_PATCH_COMMAND PATCH_COMMAND
    ${CMAKE_COMMAND}
    -DTARGET=util/mkbuildinf.pl
    -P ${CMAKE_CURRENT_LIST_DIR}/scrub_mkbuildinf.cmake
)

# openssl-cmake hardcodes mirror.viaduck.org, which now redirects to a login page. Included right before its
# ExternalProject_Add, so a second URL appended here is tried after the mirror fails the hash check.
list(APPEND OPENSSL_CHECK_HASH URL https://github.com/openssl/openssl/releases/download/openssl-${OPENSSL_BUILD_VERSION}/openssl-${OPENSSL_BUILD_VERSION}.tar.gz)
